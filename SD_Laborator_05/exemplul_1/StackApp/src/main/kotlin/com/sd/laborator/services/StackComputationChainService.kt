package com.sd.laborator.services

import com.sd.laborator.interfaces.StackComputationStep
import com.sd.laborator.model.ComputationContext
import com.sd.laborator.model.Stack
import org.springframework.stereotype.Service

@Service
class StackComputationChainService(private val steps: List<StackComputationStep>) {
    fun execute(initialA: Stack?, initialB: Stack?): ComputationContext {
        val context = ComputationContext(stackA = initialA, stackB = initialB)
        steps.forEach { step -> step.execute(context) }
        return context
    }
}
