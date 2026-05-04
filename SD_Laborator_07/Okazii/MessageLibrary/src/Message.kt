import java.text.SimpleDateFormat
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Message este obiectul comun pe care il trimit toate microserviciile intre ele.
 *
 * Pentru tema de acasa am adaugat identitatea ofertantului: nume, telefon si e-mail.
 * Formatul de pe retea este unul stabil, cu Base64 pentru campurile text:
 *   MSG2 <timestamp> <sender64> <name64> <phone64> <email64> <body64>\n
 *
 * Am ales Base64 ca sa nu se strice mesajele cand numele/body-ul contin spatii.
 * Inainte formatul era separat prin spatii, iar "Ion Popescu" era citit gresit.
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
        private const val WIRE_PREFIX = "MSG2"
        private val B64_ENCODER = Base64.getEncoder()
        private val B64_DECODER = Base64.getDecoder()
        private val BID_AMOUNT_REGEX = Regex("""\blicitez\s+(\d+)""")

        private fun encode(value: String): String =
            B64_ENCODER.encodeToString(value.toByteArray(StandardCharsets.UTF_8))

        private fun decode(value: String): String =
            String(B64_DECODER.decode(value), StandardCharsets.UTF_8)

        /**
         * Creeaza un mesaj de sistem, folosit pentru semnale ca "final".
         */
        fun create(sender: String, body: String): Message {
            return Message(sender, body, Date())
        }

        /**
         * Creeaza un mesaj de licitatie cu datele personale ale ofertantului.
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
         * Deserializeaza un mesaj primit de pe socket.
         * Accepta si formatul vechi, ca recovery-ul sa poata citi state.dat ramas
         * de la o rulare anterioara a laboratorului.
         */
        fun deserialize(msg: ByteArray): Message {
            val msgString = String(msg, StandardCharsets.UTF_8).trim()
            if (msgString.isBlank()) {
                return Message("unknown", "", Date())
            }

            if (msgString.startsWith("$WIRE_PREFIX ")) {
                return deserializeCurrentFormat(msgString)
            }

            return deserializeLegacyFormat(msgString)
        }

        private fun deserializeCurrentFormat(msgString: String): Message {
            val parts = msgString.split(' ', limit = 7)
            if (parts.size != 7) {
                return Message("unknown", msgString, Date())
            }

            return try {
                Message(
                    sender = decode(parts[2]),
                    body = decode(parts[6]),
                    timestamp = Date(parts[1].toLongOrNull() ?: System.currentTimeMillis()),
                    name = decode(parts[3]),
                    phone = decode(parts[4]),
                    email = decode(parts[5])
                )
            } catch (e: Exception) {
                return Message("unknown", msgString, Date())
            }
        }

        private fun deserializeLegacyFormat(msgString: String): Message {
            val parts = msgString.split(' ', limit = 3)
            if (parts.size < 3) {
                return Message("unknown", msgString, Date())
            }
            val timestampMs = parts[0].toLongOrNull() ?: System.currentTimeMillis()
            val sender = parts[1]
            val rest = parts[2]

            return if (rest.startsWith("[name=")) {
                // Format vechi: [name=Ion Popescu|phone=...|email=...|] licitez 5000
                val identityEnd = rest.indexOf("|]")
                if (identityEnd == -1) {
                    return Message(sender, rest, Date(timestampMs))
                }
                val identityBlock = rest.substring(1, identityEnd)
                val identityMap = identityBlock.split('|').associate { kv ->
                    val eq = kv.indexOf('=')
                    if (eq >= 0) kv.substring(0, eq) to kv.substring(eq + 1) else kv to ""
                }
                val body = rest.substring(identityEnd + 2).trimStart()
                Message(
                    sender, body, Date(timestampMs),
                    identityMap["name"] ?: "",
                    identityMap["phone"] ?: "",
                    identityMap["email"] ?: ""
                )
            } else {
                Message(sender, rest, Date(timestampMs))
            }
        }
    }

    /**
     * Verificam daca mesajul chiar contine date de ofertant.
     */
    fun hasIdentity(): Boolean = name.isNotEmpty() || phone.isNotEmpty() || email.isNotEmpty()

    /**
     * Scoatem suma din corpul mesajului fara sa presupunem ca este exact al doilea cuvant.
     * Asta ajuta si la recovery, unde pot exista mesaje vechi cu text inainte de "licitez".
     */
    fun bidAmount(): Int? = BID_AMOUNT_REGEX.find(body)?.groupValues?.getOrNull(1)?.toIntOrNull()

    /**
     * Serializeaza mesajul in formatul trimis pe socket.
     */
    fun serialize(): ByteArray {
        return "$WIRE_PREFIX ${timestamp.time} ${encode(sender)} ${encode(name)} ${encode(phone)} ${encode(email)} ${encode(body)}\n"
            .toByteArray(StandardCharsets.UTF_8)
    }

    override fun toString(): String {
        val dateString = DATE_FORMAT.format(timestamp)
        val identityStr = if (hasIdentity()) " (name=$name, phone=$phone, email=$email)" else ""
        return "[$dateString] $sender$identityStr >>> $body"
    }

    /**
     * Cheia de deduplicare reprezinta mesajul exact.
     * Daca acelasi bidder trimite acelasi obiect de doua ori, timestamp-ul ramane identic,
     * deci MessageProcessor il poate elimina fara sa confunde doua licitatii diferite.
     */
    fun deduplicationKey(): String = "${timestamp.time}|$sender|$name|$phone|$email|$body"
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
