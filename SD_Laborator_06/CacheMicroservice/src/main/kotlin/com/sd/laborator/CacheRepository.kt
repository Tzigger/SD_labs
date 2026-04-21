package com.sd.laborator

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import javax.annotation.PostConstruct

@Repository
class CacheRepository : ICacheRepository {

    @Autowired
    private lateinit var _jdbcTemplate: JdbcTemplate

    private var _rowMapper: RowMapper<CacheEntity?> = CacheRowMapper()

    @PostConstruct
    override fun createTable() {
        _jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS cache (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                query     VARCHAR(200) UNIQUE,
                result    TEXT,
                timestamp BIGINT
            )
        """.trimIndent())
    }

    override fun add(cache: CacheEntity) {
        _jdbcTemplate.update(
            "INSERT INTO cache (query, result, timestamp) VALUES (?, ?, ?)",
            cache.query, cache.result, cache.timestamp
        )
    }

    override fun getByQuery(query: String): CacheEntity? {
        val results = _jdbcTemplate.query(
            "SELECT * FROM cache WHERE query = ?",
            _rowMapper, query
        )
        return results.firstOrNull()
    }

    override fun update(cache: CacheEntity) {
        _jdbcTemplate.update(
            "UPDATE cache SET result = ?, timestamp = ? WHERE query = ?",
            cache.result, cache.timestamp, cache.query
        )
    }
}
