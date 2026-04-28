package com.sd.laborator.services.steps

import com.sd.laborator.interfaces.StackComputationStep
import com.sd.laborator.interfaces.UnionOperation
import com.sd.laborator.model.ComputationContext
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service

@Service
@Order(3)
class UnionStepService(private val unionOperation: UnionOperation): StackComputationStep {
    override fun execute(context: ComputationContext): ComputationContext {
        // Ultimul pas ruleaza doar daca pasii anteriori nu au pus eroare.
        if (context.error != null) {
            return context
        }

        // Rezultatul final este reuniunea celor doua produse calculate anterior.
        context.result = unionOperation.executeOperation(context.leftProduct, context.rightProduct)
        return context
    }
}
