package com.sd.laborator.services.steps

import com.sd.laborator.interfaces.CartesianProductOperation
import com.sd.laborator.interfaces.StackComputationStep
import com.sd.laborator.model.ComputationContext
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service

@Service
@Order(2)
class CartesianProductsStepService(
    private val cartesianProductOperation: CartesianProductOperation
): StackComputationStep {
    override fun execute(context: ComputationContext): ComputationContext {
        if (context.error != null || context.stackA == null || context.stackB == null) {
            return context
        }

        context.leftProduct = cartesianProductOperation.executeOperation(
            context.stackA!!.data,
            context.stackB!!.data
        )
        context.rightProduct = cartesianProductOperation.executeOperation(
            context.stackB!!.data,
            context.stackB!!.data
        )
        return context
    }
}
