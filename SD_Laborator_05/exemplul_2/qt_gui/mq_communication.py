import pika
import threading
from retry import retry
from pika.adapters.select_connection import SelectConnection


class RabbitMq:
    # Avem doua cozi: una pentru cereri spre Java si una pentru raspunsuri spre Python.
    config = {
        'host': 'localhost',
        'port': 5672,
        'username': 'student',
        'password': 'student',
        'exchange': 'libraryapp.direct',
        'request_routing_key': 'libraryapp.routingkey1',
        'request_queue': 'libraryapp.queue1',
        'response_routing_key': 'libraryapp.routingkey',
        'response_queue': 'libraryapp.queue'
    }
    credentials = pika.PlainCredentials(config['username'], config['password'])
    parameters = pika.ConnectionParameters(
        host=config['host'],
        port=config['port'],
        credentials=credentials
    )

    def __init__(self, ui):
        self.ui = ui

    def _execute_async_operation(self, setup_operation, timeout_seconds=10):
        # Aici folosim adaptorul asincron SelectConnection din Pika.
        # Operatia se termina cand callback-ul pune done_event.
        state = {
            'error': None,
            'done': False,
            'response': None,
            'connection': None
        }
        done_event = threading.Event()

        def on_open(connection):
            state['connection'] = connection
            setup_operation(connection, state, done_event)

        def on_open_error(connection, error):
            state['error'] = error
            done_event.set()
            connection.ioloop.stop()

        def on_connection_closed(connection, reason):
            if not state['done'] and state['error'] is None:
                state['error'] = Exception(str(reason))
            connection.ioloop.stop()

        def ioloop_runner():
            try:
                # SelectConnection lucreaza pe callback-uri si pe un ioloop, nu pe apeluri blocante.
                connection = SelectConnection(
                    parameters=self.parameters,
                    on_open_callback=on_open,
                    on_open_error_callback=on_open_error,
                    on_close_callback=on_connection_closed
                )
                state['connection'] = connection

                def on_timeout():
                    # Daca RabbitMQ nu raspunde, inchidem conexiunea ca sa nu ramana blocata aplicatia.
                    if state['done']:
                        return
                    state['error'] = TimeoutError('Timeout while waiting for RabbitMQ operation')
                    done_event.set()
                    try:
                        connection.close()
                    except Exception:
                        connection.ioloop.stop()

                connection.ioloop.call_later(timeout_seconds, on_timeout)
                connection.ioloop.start()
            except Exception as exc:
                state['error'] = exc
                done_event.set()

        worker = threading.Thread(target=ioloop_runner, daemon=True)
        worker.start()
        # GUI-ul asteapta rezultatul, dar comunicarea RabbitMQ se face prin adaptor asincron.
        done_event.wait(timeout_seconds + 2)

        connection = state.get('connection')
        if connection is not None and connection.is_open:
            try:
                connection.ioloop.add_callback_threadsafe(connection.close)
            except Exception:
                pass

        worker.join(timeout=2)

        if state['error'] is not None:
            raise state['error']
        return state.get('response')

    @retry(pika.exceptions.AMQPConnectionError, delay=5, jitter=(1, 3))
    def receive_message(self):
        def setup_consumer(connection, state, done_event):
            def on_channel_open(channel):
                def on_topology_ready():
                    def on_received_message(_channel, delivery, _properties, message):
                        try:
                            # Raspunsul venit de la Java este pastrat aici si returnat catre interfata.
                            state['response'] = message.decode('utf-8')
                            # Confirmam mesajul doar dupa ce l-am citit corect.
                            _channel.basic_ack(delivery.delivery_tag)
                        except Exception as exc:
                            state['error'] = exc
                        finally:
                            state['done'] = True
                            done_event.set()
                            try:
                                _channel.close()
                            except Exception:
                                pass
                            connection.close()

                    channel.basic_consume(
                        queue=self.config['response_queue'],
                        on_message_callback=on_received_message,
                        auto_ack=False
                    )

                self.configure_topology(channel, on_topology_ready)

            connection.channel(on_open_callback=on_channel_open)

        return self._execute_async_operation(setup_consumer)

    def send_message(self, message):
        def setup_publisher(connection, state, done_event):
            def on_channel_open(channel):
                def on_topology_ready():
                    def on_queue_purged(_frame):
                        try:
                            # Trimitem cererea catre backend pe routing key-ul de request.
                            channel.basic_publish(exchange=self.config['exchange'],
                                                  routing_key=self.config['request_routing_key'],
                                                  body=message)
                            state['done'] = True
                            done_event.set()
                        except Exception as exc:
                            state['error'] = exc
                            done_event.set()
                        finally:
                            try:
                                channel.close()
                            except Exception:
                                pass
                            connection.close()

                    # Curatam coada de raspuns ca sa nu citim un raspuns ramas de la o cerere veche.
                    channel.queue_purge(queue=self.config['response_queue'],
                                        callback=on_queue_purged)

                self.configure_topology(channel, on_topology_ready)

            connection.channel(on_open_callback=on_channel_open)

        self._execute_async_operation(setup_publisher)

    def configure_topology(self, channel, on_ready):
        def on_exchange_declared(_frame):
            # Coada pe care Java asculta cererile din Python.
            channel.queue_declare(queue=self.config['request_queue'],
                                  durable=True,
                                  callback=on_request_queue_declared)

        def on_request_queue_declared(_frame):
            channel.queue_bind(queue=self.config['request_queue'],
                               exchange=self.config['exchange'],
                               routing_key=self.config['request_routing_key'],
                               callback=on_request_queue_bound)

        def on_request_queue_bound(_frame):
            # Coada pe care Python asteapta raspunsul de la Java.
            channel.queue_declare(queue=self.config['response_queue'],
                                  durable=True,
                                  callback=on_response_queue_declared)

        def on_response_queue_declared(_frame):
            channel.queue_bind(queue=self.config['response_queue'],
                               exchange=self.config['exchange'],
                               routing_key=self.config['response_routing_key'],
                               callback=lambda _bind_frame: on_ready())

        try:
            # Exchange direct: mesajul ajunge in coada dupa routing key.
            channel.exchange_declare(exchange=self.config['exchange'],
                                     exchange_type='direct',
                                     durable=True,
                                     callback=on_exchange_declared)
        except Exception:
            raise
