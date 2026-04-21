package com.sd.laborator

import org.springframework.web.bind.annotation.*
import kotlin.math.ceil
import kotlin.math.log2

@RestController
class MerkleController {

    private var root: MerkleNode? = null
    private val dataBlocks = mutableListOf<String>()

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

    @PostMapping("/add-zone")
    fun addZone(@RequestBody data: String): String {
        dataBlocks.add(data)
        root = buildTree(dataBlocks)
        return "Zone added. Merkle Root Hash: ${root?.hash}"
    }

    @GetMapping("/search-zone")
    fun searchZone(@RequestParam hash: String): String {
        // Logica simplă pentru validare: noi doar returnăm ce block a dat match pe hash
        val found = dataBlocks.find { 
            MerkleNode(data = it).hash == hash 
        }
        return if (found != null) "Found data: $found" else "Data not found for hash"
    }
}
