package com.sd.laborator

import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.amqp.rabbit.annotation.EnableRabbit

@Configuration
@EnableRabbit
class RabbitMqConfig {

    @Bean
    fun printerFileQueue(): Queue {
        return Queue("printer.file", true)
    }

    @Bean
    fun printerCommandsQueue(): Queue {
        return Queue("printer.commands", true)
    }

    @Bean
    fun printerStatusQueue(): Queue {
        return Queue("printer.status", true)
    }
}
