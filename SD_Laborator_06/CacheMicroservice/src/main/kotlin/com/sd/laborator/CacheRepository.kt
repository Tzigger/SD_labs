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
                query_hash VARCHAR(64),
                result    TEXT,
                result_hash VARCHAR(64),
                timestamp BIGINT
            )
        """.trimIndent())
        ensureColumn("query_hash", "VARCHAR(64)")
        ensureColumn("result_hash", "VARCHAR(64)")
        _jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_cache_query_hash ON cache(query_hash)")
    }

    override fun add(cache: CacheEntity) {
        _jdbcTemplate.update(
            "INSERT INTO cache (query, query_hash, result, result_hash, timestamp) VALUES (?, ?, ?, ?, ?)",
            cache.query, cache.queryHash, cache.result, cache.resultHash, cache.timestamp
        )
    }

    override fun getByQuery(query: String): CacheEntity? {
        val results = _jdbcTemplate.query(
            "SELECT * FROM cache WHERE query = ?",
            _rowMapper, query
        )
        return results.firstOrNull()
    }

    override fun getByQueryHash(queryHash: String): CacheEntity? {
        val results = _jdbcTemplate.query(
            "SELECT * FROM cache WHERE query_hash = ?",
            _rowMapper, queryHash
        )
        return results.firstOrNull()
    }

    override fun update(cache: CacheEntity) {
        _jdbcTemplate.update(
            "UPDATE cache SET query_hash = ?, result = ?, result_hash = ?, timestamp = ? WHERE query = ?",
            cache.queryHash, cache.result, cache.resultHash, cache.timestamp, cache.query
        )
    }

    private fun ensureColumn(name: String, definition: String) {
        val columns = _jdbcTemplate.queryForList("PRAGMA table_info(cache)")
        val exists = columns.any { it["name"] == name }
        if (!exists) {
            _jdbcTemplate.execute("ALTER TABLE cache ADD COLUMN $name $definition")
        }
    }
}
