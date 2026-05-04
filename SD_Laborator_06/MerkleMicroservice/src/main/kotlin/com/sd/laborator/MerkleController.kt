package com.sd.laborator

import org.springframework.web.bind.annotation.*

@RestController
class MerkleController {

    private var root: MerkleNode? = null
    private val dataBlocks = mutableListOf<String>()
    private val cacheEntries = linkedMapOf<String, String>()

    private fun buildTree(blocks: List<String>): MerkleNode? {
        if (blocks.isEmpty()) return null
        var nodes = blocks.map { MerkleNode(data = it) }

        while (nodes.size > 1) {
            val nextLevel = mutableListOf<MerkleNode>()
            for (i in nodes.indices step 2) {
                if (i + 1 < nodes.size) {
                    nextLevel.add(MerkleNode(left = nodes[i], right = nodes[i+1]))
                } else {
                    nextLevel.add(nodes[i])
                }
            }
            nodes = nextLevel
        }
        return nodes.firstOrNull()
    }

    private fun rebuildTree() {
        val cacheBlocks = cacheEntries.map { "${it.key}:${it.value}" }
        root = buildTree(dataBlocks + cacheBlocks)
    }

    @PostMapping("/add-zone")
    fun addZone(@RequestBody data: String): String {
        dataBlocks.add(data)
        rebuildTree()
        return "Zone added. Merkle Root Hash: ${root?.hash}"
    }

    @PostMapping("/index-zone")
    fun indexZone(@RequestBody payload: Map<String, String>): String {
        val queryHash = payload["queryHash"] ?: return "Invalid zone: queryHash missing"
        val resultHash = payload["resultHash"] ?: return "Invalid zone: resultHash missing"
        cacheEntries[queryHash] = resultHash
        rebuildTree()
        return "Zone indexed. Query Hash: $queryHash Merkle Root Hash: ${root?.hash}"
    }

    @GetMapping("/lookup-zone")
    fun lookupZone(@RequestParam queryHash: String): String {
        val resultHash = cacheEntries[queryHash]
        return if (resultHash != null) "FOUND:$resultHash" else "MISS"
    }

    @GetMapping("/search-zone")
    fun searchZone(@RequestParam hash: String): String {
        val foundCache = cacheEntries.entries.find {
            it.key == hash || MerkleNode(data = "${it.key}:${it.value}").hash == hash
        }
        if (foundCache != null) {
            return "Found cache queryHash=${foundCache.key} resultHash=${foundCache.value}"
        }

        val found = dataBlocks.find { 
            MerkleNode(data = it).hash == hash 
        }
        return if (found != null) "Found data: $found" else "Data not found for hash"
    }
}
