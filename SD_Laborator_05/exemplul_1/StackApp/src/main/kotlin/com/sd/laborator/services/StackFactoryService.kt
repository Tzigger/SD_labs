package com.sd.laborator.services

import com.sd.laborator.interfaces.PrimeNumberGenerator
import com.sd.laborator.model.Stack
import org.springframework.stereotype.Service

@Service
class StackFactoryService(private val primeGenerator: PrimeNumberGenerator) {
    fun generateStack(count: Int): Stack? {
        if (count < 1) {
            return null
        }

        val data: MutableSet<Int> = mutableSetOf()
        while (data.count() < count) {
            data.add(primeGenerator.generatePrimeNumber())
        }

        return Stack(data)
    }
}
