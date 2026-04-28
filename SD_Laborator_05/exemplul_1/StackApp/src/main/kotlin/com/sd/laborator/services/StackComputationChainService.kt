package com.sd.laborator.services

import com.sd.laborator.interfaces.StackComputationStep
import com.sd.laborator.model.ComputationContext
import com.sd.laborator.model.Stack
import org.springframework.stereotype.Service

@Service
class StackComputationChainService(private val steps: List<StackComputationStep>) {
    fun execute(initialA: Stack?, initialB: Stack?): ComputationContext {
        // Pornim de la multimile curente si le punem intr-un context comun.
        val context = ComputationContext(stackA = initialA, stackB = initialB)

        // Spring injecteaza pasii in ordinea data de @Order.
        // Fiecare pas modifica acelasi context si apoi il lasa pentru urmatorul.
        steps.forEach { step -> step.execute(context) }

        // La final, contextul contine si multimile, si rezultatul calculului.
        return context
    }
}
