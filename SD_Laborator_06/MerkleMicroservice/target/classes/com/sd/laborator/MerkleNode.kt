package com.sd.laborator

import java.security.MessageDigest

class MerkleNode(val data: String? = null, val left: MerkleNode? = null, val right: MerkleNode? = null) {
    val hash: String

    init {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = if (data != null) data else (left?.hash ?: "") + (right?.hash ?: "")
        val bytes = digest.digest(input.toByteArray())
        hash = bytes.joinToString("") { "%02x".format(it) }
    }
}
