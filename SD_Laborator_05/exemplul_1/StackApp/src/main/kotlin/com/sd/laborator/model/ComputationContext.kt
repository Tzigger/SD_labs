package com.sd.laborator.model

data class ComputationContext(
    // Contextul tine toate datele care trec prin chain.
    var stackA: Stack? = null,
    var stackB: Stack? = null,
    var leftProduct: Set<Pair<Int, Int>> = emptySet(),
    var rightProduct: Set<Pair<Int, Int>> = emptySet(),
    var result: Set<Pair<Int, Int>> = emptySet(),
    var error: String? = null
)
