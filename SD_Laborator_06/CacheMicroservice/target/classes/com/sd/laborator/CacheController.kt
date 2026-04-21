package com.sd.laborator

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
class CacheController {

    @Autowired
    private lateinit var cacheRepository: ICacheRepository

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    // 1 ora in ms
    private val EXPIRATION_TIME_MS: Long = 3600 * 1000

    @GetMapping("/get-cache")
    fun getCache(@RequestParam query: String): String {
        val cache = cacheRepository.getByQuery(query)
        if (cache != null) {
            val now = System.currentTimeMillis()
            if (now - cache.timestamp <= EXPIRATION_TIME_MS) {
                // HIT -> Send to printer queues as requested by homework
                rabbitTemplate.convertAndSend("printer.file", cache.result)
                rabbitTemplate.convertAndSend("printer.commands", "PRINT FORMAT=JSON")
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
        val existing = cacheRepository.getByQuery(query)

        if (existing == null) {
            cacheRepository.add(CacheEntity(0, query, result, now))
        } else {
            existing.result = result
            existing.timestamp = now
            cacheRepository.update(existing)
        }

        // Send to MerkleMicroservice
        try {
            val merkleUrl = java.net.URL("http://localhost:8082/add-zone")
            val conn = merkleUrl.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8")
            conn.outputStream.write(result.toByteArray(Charsets.UTF_8))
            conn.outputStream.close()
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            println("Merkle error: ${e.message}")
        }

        return "Saved to cache"
    }
}
