package com.sd.laborator.components

import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class RabbitMqConnectionFactoryComponent {
    @Value("\${spring.rabbitmq.host}")
    private lateinit var host: String
    @Value("\${spring.rabbitmq.port}")
    private val port: Int = 0
    @Value("\${spring.rabbitmq.username}")
    private lateinit var username: String
    @Value("\${spring.rabbitmq.password}")
    private lateinit var password: String
    @Value("\${libraryapp.rabbitmq.exchange}")
    private lateinit var exchange: String
    @Value("\${libraryapp.rabbitmq.routingkey}")
    private lateinit var routingKey: String
    @Value("\${libraryapp.rabbitmq.queue}")
    private lateinit var queueName: String
    @Value("\${libraryapp.rabbitmq.request.routingkey}")
    private lateinit var requestRoutingKey: String
    @Value("\${libraryapp.rabbitmq.response.queue}")
    private lateinit var responseQueueName: String
    @Value("\${libraryapp.rabbitmq.response.routingkey}")
    private lateinit var responseRoutingKey: String

    fun getExchange(): String = this.exchange

    // Backend-ul trimite inapoi raspunsurile pe routing key-ul de response.
    fun getRoutingKey(): String = this.responseRoutingKey

    @Bean
    fun connectionFactory(): ConnectionFactory {
        val connectionFactory = CachingConnectionFactory()
        connectionFactory.host = host
        connectionFactory.username = username
        connectionFactory.setPassword(password)
        connectionFactory.port = port
        return connectionFactory
    }

    @Bean
    fun amqpAdmin(): AmqpAdmin = RabbitAdmin(connectionFactory())

    @Bean
    fun rabbitTemplate(): RabbitTemplate = RabbitTemplate(this.connectionFactory())

    @Bean
    // Coada de request: aici ajung comenzile trimise de aplicatia Python.
    fun requestQueue(): Queue = Queue(this.queueName, true)

    @Bean
    // Coada de response: aici pune Java raspunsul pe care il citeste Python.
    fun responseQueue(): Queue = Queue(this.responseQueueName, true)

    @Bean
    fun exchangeBean(): DirectExchange = DirectExchange(this.exchange)

    @Bean
    fun requestBinding(
        @Qualifier("requestQueue") requestQueue: Queue,
        exchangeBean: DirectExchange
    ): Binding =
        // Legam coada de request de exchange prin routing key-ul de request.
        BindingBuilder.bind(requestQueue).to(exchangeBean).with(this.requestRoutingKey)

    @Bean
    fun responseBinding(
        @Qualifier("responseQueue") responseQueue: Queue,
        exchangeBean: DirectExchange
    ): Binding =
        // Legam coada de response de exchange prin routing key-ul de response.
        BindingBuilder.bind(responseQueue).to(exchangeBean).with(this.responseRoutingKey)
}
