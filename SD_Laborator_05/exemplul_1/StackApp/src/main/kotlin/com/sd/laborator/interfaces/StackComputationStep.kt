package com.sd.laborator.interfaces

import com.sd.laborator.model.ComputationContext

interface StackComputationStep {
    // Fiecare pas primeste acelasi context si adauga in el rezultatul lui.
    fun execute(context: ComputationContext): ComputationContext
}
