package com.sd.laborator.components

import com.sd.laborator.model.Stack
import com.sd.laborator.services.StackComputationChainService
import com.sd.laborator.services.StackFactoryService
import org.springframework.amqp.core.AmqpTemplate
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class StackAppComponent {
    private var A: Stack? = null
    private var B: Stack? = null

    @Autowired
    private lateinit var computationChain: StackComputationChainService
    @Autowired
    private lateinit var stackFactory: StackFactoryService
    @Autowired
    private lateinit var connectionFactory: RabbitMqConnectionFactoryComponent

    private lateinit var amqpTemplate: AmqpTemplate

    @Autowired
    fun initTemplate() {
        this.amqpTemplate = connectionFactory.rabbitTemplate()
    }

    @RabbitListener(queues = ["\${stackapp.rabbitmq.queue}"])
    fun recieveMessage(msg: String) {
        val processedMsg = decodeIncomingMessage(msg)
        val result: String? = try {
            when(processedMsg) {
                "compute" -> computeExpression()
                "regenerate_A" -> regenerateA()
                "regenerate_B" -> regenerateB()
                else -> null
            }
        } catch (e: Exception) {
            "compute~Error: ${e.message}"
        }
        println("result: ")
        println(result)
        if (result != null) sendMessage(result)
    }

    fun sendMessage(msg: String) {
        println("message: ")
        println(msg)
        this.amqpTemplate.convertAndSend(connectionFactory.getExchange(),
            connectionFactory.getRoutingKey(),
            msg)
    }

    private fun computeExpression(): String {
        val context = computationChain.execute(A, B)
        A = context.stackA
        B = context.stackB

        if (context.error != null) {
            return "compute~Error: ${context.error}"
        }

        return "compute~" + "{\"A\": \"" + A?.data.toString() +"\", \"B\": \"" + B?.data.toString() + "\", \"result\": \"" + context.result.toString() + "\"}"
    }

    private fun regenerateA(): String {
        A = stackFactory.generateStack(20)
        return "A~" + A?.data.toString()
    }

    private fun regenerateB(): String {
        B = stackFactory.generateStack(20)
        return "B~" + B?.data.toString()
    }

    private fun decodeIncomingMessage(msg: String): String {
        return try {
            val chunks = msg.split(",")
            val looksEncoded = chunks.size > 1 && chunks.all { it.trim().matches(Regex("\\d+")) }
            if (looksEncoded) {
                chunks.map { it.trim().toInt().toChar() }.joinToString(separator = "")
            } else {
                msg
            }
        } catch (_: Exception) {
            msg
        }
    }
}
