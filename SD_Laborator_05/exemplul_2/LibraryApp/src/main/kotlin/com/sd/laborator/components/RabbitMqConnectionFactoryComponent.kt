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

    fun getExchange(): String = this.exchange

    fun getRoutingKey(): String = this.routingKey

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
    fun queue(): Queue = Queue(this.queueName, false)

    @Bean
    fun exchangeBean(): DirectExchange = DirectExchange(this.exchange)

    @Bean
    fun binding(queue: Queue, exchangeBean: DirectExchange): Binding =
        BindingBuilder.bind(queue).to(exchangeBean).with(this.routingKey)
}