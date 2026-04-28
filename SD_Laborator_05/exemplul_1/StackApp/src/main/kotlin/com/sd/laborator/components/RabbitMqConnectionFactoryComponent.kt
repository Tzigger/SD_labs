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
    @Value("\${stackapp.rabbitmq.exchange}")
    private lateinit var exchange: String
    @Value("\${stackapp.rabbitmq.routingkey}")
    private lateinit var routingKey: String
    @Value("\${stackapp.rabbitmq.queue}")
    private lateinit var queueName: String
    @Value("\${stackapp.rabbitmq.request.routingkey}")
    private lateinit var requestRoutingKey: String
    @Value("\${stackapp.rabbitmq.response.queue}")
    private lateinit var responseQueueName: String
    @Value("\${stackapp.rabbitmq.response.routingkey}")
    private lateinit var responseRoutingKey: String

    fun getExchange(): String = this.exchange

    fun getRoutingKey(): String = this.responseRoutingKey

    @Bean
    fun connectionFactory(): ConnectionFactory {
        val connectionFactory = CachingConnectionFactory()
        connectionFactory.host = this.host
        connectionFactory.username = this.username
        connectionFactory.setPassword(this.password)
        connectionFactory.port = this.port
        return connectionFactory
    }

    @Bean
    fun amqpAdmin(): AmqpAdmin = RabbitAdmin(connectionFactory())

    @Bean
    fun rabbitTemplate(): RabbitTemplate = RabbitTemplate(connectionFactory())

    @Bean
    fun requestQueue(): Queue = Queue(this.queueName, true)

    @Bean
    fun responseQueue(): Queue = Queue(this.responseQueueName, true)

    @Bean
    fun exchangeBean(): DirectExchange = DirectExchange(this.exchange)

    @Bean
    fun requestBinding(
        @Qualifier("requestQueue") requestQueue: Queue,
        exchangeBean: DirectExchange
    ): Binding =
        BindingBuilder.bind(requestQueue).to(exchangeBean).with(this.requestRoutingKey)

    @Bean
    fun responseBinding(
        @Qualifier("responseQueue") responseQueue: Queue,
        exchangeBean: DirectExchange
    ): Binding =
        BindingBuilder.bind(responseQueue).to(exchangeBean).with(this.responseRoutingKey)
}
