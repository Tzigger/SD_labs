package com.sd.laborator

class CacheEntity(
    var id: Int,
    var query: String,
    var queryHash: String,
    var result: String,
    var resultHash: String,
    var timestamp: Long
)
