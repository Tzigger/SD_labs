package com.sd.laborator

interface ICacheRepository {
    fun createTable()
    fun add(cache: CacheEntity)
    fun getByQuery(query: String): CacheEntity?
    fun getByQueryHash(queryHash: String): CacheEntity?
    fun update(cache: CacheEntity)
}
