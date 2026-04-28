package com.sd.laborator.services.steps

import com.sd.laborator.interfaces.StackComputationStep
import com.sd.laborator.model.ComputationContext
import com.sd.laborator.services.StackFactoryService
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service

@Service
@Order(1)
class EnsureStacksStepService(private val stackFactory: StackFactoryService): StackComputationStep {
    override fun execute(context: ComputationContext): ComputationContext {
        // Primul pas se asigura ca avem ambele multimi generate.
        if (context.stackA == null) {
            context.stackA = stackFactory.generateStack(20)
        }
        if (context.stackB == null) {
            context.stackB = stackFactory.generateStack(20)
        }

        if (context.stackA == null || context.stackB == null) {
            // Daca ceva nu s-a putut genera, oprim restul chain-ului prin eroare.
            context.error = "A sau B este null"
            return context
        }

        if (context.stackA!!.data.count() != context.stackB!!.data.count()) {
            // Restul pasilor verifica acest camp si nu mai continua daca exista eroare.
            context.error = "A.count() != B.count()"
        }

        return context
    }
}
