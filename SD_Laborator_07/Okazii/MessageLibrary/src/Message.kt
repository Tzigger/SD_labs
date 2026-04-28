import java.text.SimpleDateFormat
import java.util.*

/**
 * Represents a message exchanged between microservices in the Okazii auction system.
 *
 * Extended for homework: includes optional bidder identity fields (name, phone, email).
 * The serialized format is:
 *   <timestamp> <sender> [name=<name>|phone=<phone>|email=<email>|] <body>\n
 *
 * For system/control messages (no identity), the identity block is omitted.
 */
class Message private constructor(
    val sender: String,
    val body: String,
    val timestamp: Date,
    val name: String = "",
    val phone: String = "",
    val email: String = ""
) {
    companion object {
        private val DATE_FORMAT = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")

        /**
         * Create a plain system message (no bidder identity).
         */
        fun create(sender: String, body: String): Message {
            return Message(sender, body, Date())
        }

        /**
         * Create a message with bidder identity fields (name, phone, email).
         */
        fun createWithIdentity(
            sender: String,
            body: String,
            name: String,
            phone: String,
            email: String
        ): Message {
            return Message(sender, body, Date(), name, phone, email)
        }

        /**
         * Deserialize a message from its wire-format byte array.
         * Format: "<timestamp> <sender> [name=<n>|phone=<p>|email=<e>|] <body>\n"
         * Identity block is present only when all three identity fields are non-empty.
         */
        fun deserialize(msg: ByteArray): Message {
            val msgString = String(msg).trim()
            val parts = msgString.split(' ', limit = 4)

            if (parts.size < 3) {
                // Fallback: return a minimal message to avoid crashes on malformed input
                return Message("unknown", msgString, Date())
            }

            val timestampMs = parts[0].toLongOrNull() ?: System.currentTimeMillis()
            val sender = parts[1]

            return if (parts.size == 4 && parts[2].startsWith("[name=")) {
                // Identity block present: "[name=<n>|phone=<p>|email=<e>|]"
                val identityBlock = parts[2].removePrefix("[").removeSuffix("|]")
                val identityMap = identityBlock.split('|').associate { kv ->
                    val eq = kv.indexOf('=')
                    if (eq >= 0) kv.substring(0, eq) to kv.substring(eq + 1) else kv to ""
                }
                val body = parts[3]
                Message(
                    sender, body, Date(timestampMs),
                    identityMap["name"] ?: "",
                    identityMap["phone"] ?: "",
                    identityMap["email"] ?: ""
                )
            } else {
                // No identity block — body is everything from parts[2] onward
                val body = if (parts.size == 4) "${parts[2]} ${parts[3]}" else parts[2]
                Message(sender, body, Date(timestampMs))
            }
        }
    }

    /**
     * Returns true if this message carries bidder identity information.
     */
    fun hasIdentity(): Boolean = name.isNotEmpty() || phone.isNotEmpty() || email.isNotEmpty()

    /**
     * Serialize the message to its wire-format byte array.
     */
    fun serialize(): ByteArray {
        val identityPart = if (hasIdentity()) {
            " [name=$name|phone=$phone|email=$email|]"
        } else {
            ""
        }
        return "${timestamp.time} $sender$identityPart $body\n".toByteArray()
    }

    override fun toString(): String {
        val dateString = DATE_FORMAT.format(timestamp)
        val identityStr = if (hasIdentity()) " (name=$name, phone=$phone, email=$email)" else ""
        return "[$dateString] $sender$identityStr >>> $body"
    }

    /**
     * A key used for duplicate detection: same sender + same body = duplicate.
     */
    fun deduplicationKey(): String = "$sender|$body"
}

fun main(args: Array<String>) {
    // Plain message test
    val msg = Message.create("localhost:4848", "test mesaj")
    println(msg)
    val serialized = msg.serialize()
    val deserialized = Message.deserialize(serialized)
    println(deserialized)

    // Identity message test
    val identityMsg = Message.createWithIdentity(
        "localhost:5050", "licitez 5000",
        name = "Ion Popescu", phone = "0740123456", email = "ion@example.com"
    )
    println(identityMsg)
    val serializedIdentity = identityMsg.serialize()
    println("Serialized: ${String(serializedIdentity).trim()}")
    val deserializedIdentity = Message.deserialize(serializedIdentity)
    println(deserializedIdentity)
}