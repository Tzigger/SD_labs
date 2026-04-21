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
        // the result: 114,101,103,101,110,101,114,97,116,101,95,65 --> needs processing
        val processed_msg = (msg.split(",").map { it.toInt().toChar() }).joinToString(separator="")
        var result: String? = when(processed_msg) {
            "compute" -> computeExpression()
            "regenerate_A" -> regenerateA()
            "regenerate_B" -> regenerateB()
            else -> null
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
}
