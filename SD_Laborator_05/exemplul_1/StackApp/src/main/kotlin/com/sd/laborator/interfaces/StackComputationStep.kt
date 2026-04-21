package com.sd.laborator.interfaces

import com.sd.laborator.model.ComputationContext

interface StackComputationStep {
    fun execute(context: ComputationContext): ComputationContext
}
