import pika
import threading
from retry import retry
from pika.adapters.select_connection import SelectConnection


class RabbitMq:
    config = {
        'host': 'localhost',
        'port': 5672,
        'username': 'student',
        'password': 'student',
        'exchange': 'libraryapp.direct',
        'routing_key': 'libraryapp.routingkey',
        'queue': 'libraryapp.queue1'
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
                connection = SelectConnection(
                    parameters=self.parameters,
                    on_open_callback=on_open,
                    on_open_error_callback=on_open_error,
                    on_close_callback=on_connection_closed
                )
                state['connection'] = connection

                def on_timeout():
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
                def on_received_message(_channel, delivery, _properties, message):
                    try:
                        state['response'] = message.decode('utf-8')
                        self.ui.set_response(state['response'])
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
                    queue=self.config['queue'],
                    on_message_callback=on_received_message,
                    auto_ack=False
                )

            connection.channel(on_open_callback=on_channel_open)

        return self._execute_async_operation(setup_consumer)

    def send_message(self, message):
        def setup_publisher(connection, state, done_event):
            def on_channel_open(channel):
                def on_queue_purged(_frame):
                    try:
                        channel.basic_publish(exchange=self.config['exchange'],
                                              routing_key=self.config['routing_key'],
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

                channel.queue_purge(queue=self.config['queue'], callback=on_queue_purged)

            connection.channel(on_open_callback=on_channel_open)

        self._execute_async_operation(setup_publisher)
