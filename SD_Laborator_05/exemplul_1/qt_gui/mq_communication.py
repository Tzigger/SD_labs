import pika
from retry import retry


class RabbitMq:
    config = {
        'host': 'localhost',
        'port': 5672,
        'username': 'student',
        'password': 'student',
        'exchange': 'stackapp.direct',
        'request_routing_key': 'stackapp.routingkey1',
        'request_queue': 'stackapp.queue1',
        'response_routing_key': 'stackapp.routingkey',
        'response_queue': 'stackapp.queue'
    }
    credentials = pika.PlainCredentials(config['username'], config['password'])
    parameters = pika.ConnectionParameters(
        host=config['host'],
        port=config['port'],
        credentials=credentials
    )

    def __init__(self, ui):
        self.ui = ui

    def on_received_message(self, blocking_channel, deliver, properties,
                            message):
        result = message.decode('utf-8')
        blocking_channel.basic_ack(deliver.delivery_tag)
        try:
            variable, response = result.split('~')
            self.ui.set_response(variable, response)
        except Exception as e:
            print(e)
            print("wrong data format")
        finally:
            blocking_channel.stop_consuming()

    @retry(pika.exceptions.AMQPConnectionError, delay=5, jitter=(1, 3))
    def receive_message(self):
        # automatically close the connection
        with pika.BlockingConnection(self.parameters) as connection:
            # automatically close the channel
            with connection.channel() as channel:
                self.configure_topology(channel)
                channel.basic_consume(self.config['response_queue'],
                                      self.on_received_message)
                try:
                    channel.start_consuming()
                # Don't recover connections closed by server
                except pika.exceptions.ConnectionClosedByBroker:
                    print("Connection closed by broker.")
                # Don't recover on channel errors
                except pika.exceptions.AMQPChannelError:
                    print("AMQP Channel Error")
                # Don't recover from KeyboardInterrupt
                except KeyboardInterrupt:
                    print("Application closed.")

    def send_message(self, message):
        # automatically close the connection
        with pika.BlockingConnection(self.parameters) as connection:
            # automatically close the channel
            with connection.channel() as channel:
                self.configure_topology(channel)
                self.clear_response_queue(channel)
                channel.basic_publish(exchange=self.config['exchange'],
                                      routing_key=self.config['request_routing_key'],
                                      body=message)

    def configure_topology(self, channel):
        channel.exchange_declare(exchange=self.config['exchange'],
                                 exchange_type='direct',
                                 durable=True)
        channel.queue_declare(queue=self.config['request_queue'], durable=True)
        channel.queue_bind(queue=self.config['request_queue'],
                           exchange=self.config['exchange'],
                           routing_key=self.config['request_routing_key'])
        channel.queue_declare(queue=self.config['response_queue'], durable=True)
        channel.queue_bind(queue=self.config['response_queue'],
                           exchange=self.config['exchange'],
                           routing_key=self.config['response_routing_key'])

    def clear_response_queue(self, channel):
        channel.queue_purge(self.config['response_queue'])
