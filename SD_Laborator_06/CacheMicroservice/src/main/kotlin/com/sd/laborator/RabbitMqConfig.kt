package com.sd.laborator

import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMqConfig {

    @Bean
    fun printerFileQueue(): Queue {
        return Queue("printer.file", false)
    }

    @Bean
    fun printerCommandsQueue(): Queue {
        return Queue("printer.commands", false)
    }

    @Bean
    fun printerStatusQueue(): Queue {
        return Queue("printer.status", false)
    }
}
