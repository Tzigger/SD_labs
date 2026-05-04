package com.sd.laborator

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

@RestController
class CacheController {

    @Autowired
    private lateinit var cacheRepository: ICacheRepository

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    // 1 ora in ms
    private val EXPIRATION_TIME_MS: Long = 3600 * 1000
    private val VALID_FORMATS = setOf("json", "html", "raw")

    @Volatile
    private var lastPrinterStatus: String = "UNKNOWN"

    @GetMapping("/get-cache")
    fun getCache(@RequestParam query: String): String {
        val queryHash = sha256(query)
        val merkleHit = isIndexedInMerkle(queryHash)
        val cache = cacheRepository.getByQueryHash(queryHash)
            ?: cacheRepository.getByQuery(query)

        if (cache != null) {
            normalizeHashes(cache, query, queryHash)
            val now = System.currentTimeMillis()
            if (now - cache.timestamp <= EXPIRATION_TIME_MS) {
                if (!merkleHit) {
                    indexInMerkle(cache.queryHash, cache.resultHash)
                }
                sendToPrinter(cache, extractFormat(query))
                return "HIT: ${cache.result}"
            } else {
                return "MISS (Expired)"
            }
        }
        return "MISS"
    }

    @PostMapping("/set-cache")
    fun setCache(@RequestParam query: String, @RequestBody result: String): String {
        val now = System.currentTimeMillis()
        val queryHash = sha256(query)
        val resultHash = sha256(result)
        val existing = cacheRepository.getByQueryHash(queryHash)
            ?: cacheRepository.getByQuery(query)

        if (existing == null) {
            cacheRepository.add(CacheEntity(0, query, queryHash, result, resultHash, now))
        } else {
            existing.query = query
            existing.queryHash = queryHash
            existing.result = result
            existing.resultHash = resultHash
            existing.timestamp = now
            cacheRepository.update(existing)
        }

        indexInMerkle(queryHash, resultHash)
        return "Saved to cache"
    }

    @RabbitListener(queues = ["printer.status"])
    fun receivePrinterStatus(status: String) {
        lastPrinterStatus = status
        println("Printer status received: $status")
    }

    @GetMapping("/printer-status")
    fun getPrinterStatus(): String {
        return lastPrinterStatus
    }

    private fun sendToPrinter(cache: CacheEntity, format: String) {
        try {
            rabbitTemplate.convertAndSend("printer.file", cache.result)
            rabbitTemplate.convertAndSend(
                "printer.commands",
                "PRINT FORMAT=${format.uppercase()} QUERY_HASH=${cache.queryHash}"
            )
        } catch (e: Exception) {
            println("RabbitMQ printer send failed: ${e.message}")
        }
    }

    private fun isIndexedInMerkle(queryHash: String): Boolean {
        return try {
            val lookupUrl = URL(
                "http://localhost:8082/lookup-zone?queryHash=${URLEncoder.encode(queryHash, "UTF-8")}"
            )
            val conn = lookupUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 1000
            conn.readTimeout = 1000
            conn.responseCode == 200 && conn.inputStream.bufferedReader().use { it.readText() }.startsWith("FOUND:")
        } catch (e: Exception) {
            println("Merkle lookup error: ${e.message}")
            false
        }
    }

    private fun indexInMerkle(queryHash: String, resultHash: String) {
        try {
            val merkleUrl = URL("http://localhost:8082/index-zone")
            val conn = merkleUrl.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 1000
            conn.readTimeout = 1000
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val payload = """{"queryHash":"$queryHash","resultHash":"$resultHash"}"""
            conn.outputStream.write(payload.toByteArray(Charsets.UTF_8))
            conn.outputStream.close()
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            println("Merkle error: ${e.message}")
        }
    }

    private fun normalizeHashes(cache: CacheEntity, query: String, queryHash: String) {
        val resultHash = sha256(cache.result)
        if (cache.queryHash != queryHash || cache.resultHash != resultHash) {
            cache.query = query
            cache.queryHash = queryHash
            cache.resultHash = resultHash
            cacheRepository.update(cache)
        }
    }

    private fun extractFormat(query: String): String {
        return query.split("&")
            .mapNotNull {
                val parts = it.split("=", limit = 2)
                if (parts.firstOrNull() == "format") parts.getOrNull(1) else null
            }
            .firstOrNull()
            ?.lowercase()
            ?.takeIf { it in VALID_FORMATS }
            ?: "json"
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
