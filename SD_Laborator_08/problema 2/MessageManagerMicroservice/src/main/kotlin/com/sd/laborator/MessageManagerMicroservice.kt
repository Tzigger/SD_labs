package com.sd.laborator

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

// Subscriber identificat logic (teacher/studentX), nu doar prin portul de conexiune.
data class SubscriberSession(
    val id: String,
    val role: String,
    val socket: Socket,
    val connectionPort: Int
)

// Contextul unei intrebari active, necesar pentru a decide livrarea raspunsurilor.
data class QuestionContext(
    val requesterId: String,
    val recipientIds: Set<String>,
    val visibility: String,
    val responders: MutableSet<String> = mutableSetOf()
)

class MessageManagerMicroservice {
    private val stateLock = Any()
    // Registru de subscriberi indexat atat dupa id logic, cat si dupa portul de conexiune.
    private val subscribersById: HashMap<String, SubscriberSession>
    private val subscribersByPort: HashMap<Int, String>
    // Intrebari aflate in tranzit (requestId -> context), pentru corelarea raspunsurilor.
    private val questionContexts: HashMap<String, QuestionContext>
    private lateinit var messageManagerSocket: ServerSocket

    companion object Constants {
        const val MESSAGE_MANAGER_PORT = 1500
    }

    init {
        subscribersById = hashMapOf()
        subscribersByPort = hashMapOf()
        questionContexts = hashMapOf()
    }

    private fun registerSubscriber(id: String, role: String, socket: Socket, connectionPort: Int) {
        synchronized(stateLock) {
            subscribersById[id] = SubscriberSession(id, role, socket, connectionPort)
            subscribersByPort[connectionPort] = id
        }
    }

    private fun unregisterSubscriber(connectionPort: Int) {
        val removedId: String?
        synchronized(stateLock) {
            removedId = subscribersByPort.remove(connectionPort)
            removedId?.let { subscribersById.remove(it) }

            if (removedId != null) {
                val contextsToRemove = questionContexts.filter { (_, context) ->
                    context.requesterId == removedId || context.recipientIds.contains(removedId)
                }.keys
                contextsToRemove.forEach { questionContexts.remove(it) }
            }
        }

        removedId?.let {
            println("Subscriber-ul $it ($connectionPort) a fost deconectat.")
        }
    }

    private fun sendToSubscriber(subscriberId: String, message: String) {
        val socket = synchronized(stateLock) { subscribersById[subscriberId]?.socket } ?: return
        try {
            synchronized(socket) {
                socket.getOutputStream().write((message + "\n").toByteArray())
            }
        } catch (e: Exception) {
            println("Nu pot trimite mesaj catre $subscriberId: ${e.message}")
        }
    }

    private fun resolveRecipients(senderId: String, targetType: String, targetValue: String): Set<String> {
        // Routing explicit pentru one-to-one / one-to-many.
        return synchronized(stateLock) {
            when (targetType.uppercase()) {
                "TEACHER" -> subscribersById.values
                    .filter { it.role == "TEACHER" && it.id != senderId }
                    .map { it.id }
                    .toSet()
                "ALL_STUDENTS" -> subscribersById.values
                    .filter { it.role == "STUDENT" && it.id != senderId }
                    .map { it.id }
                    .toSet()
                "STUDENT" -> subscribersById[targetValue]
                    ?.takeIf { it.role == "STUDENT" && it.id != senderId }
                    ?.let { setOf(it.id) } ?: emptySet()
                "ALL" -> subscribersById.values
                    .filter { it.id != senderId }
                    .map { it.id }
                    .toSet()
                else -> emptySet()
            }
        }
    }

    private fun handleQuestion(
        senderId: String,
        requestId: String,
        targetType: String,
        targetValue: String,
        questionBodyBase64: String
    ) {
        val recipients = resolveRecipients(senderId, targetType, targetValue)
        // Cerinta laborator:
        // - 1 destinatar -> PRIVATE (one-to-one)
        // - mai multi destinatari -> PUBLIC (one-to-many)
        val visibility = when (recipients.size) {
            0 -> "NONE"
            1 -> "PRIVATE"
            else -> "PUBLIC"
        }

        if (recipients.isNotEmpty()) {
            synchronized(stateLock) {
                questionContexts[requestId] = QuestionContext(
                    requesterId = senderId,
                    recipientIds = recipients,
                    visibility = visibility
                )
            }
        }

        // Notificam initiatorul imediat despre vizibilitate + numarul de destinatari rezolvati.
        sendToSubscriber(senderId, "QUESTION_STATUS|$requestId|$visibility|${recipients.size}")
        if (recipients.isEmpty()) {
            return
        }

        // Distribuim intrebarea doar catre destinatarii selectati de routing.
        val deliveryMessage = "QUESTION_DELIVERY|$requestId|$senderId|$questionBodyBase64"
        recipients.forEach { recipientId ->
            sendToSubscriber(recipientId, deliveryMessage)
        }
    }

    private fun handleAnswer(senderId: String, requestId: String, answerBodyBase64: String) {
        val context = synchronized(stateLock) { questionContexts[requestId] } ?: return
        val deliveryMessage = "ANSWER_DELIVERY|$requestId|$senderId|${context.visibility}|$answerBodyBase64"

        // PUBLIC: raspunsul este vizibil in conversatie pentru initiator + destinatari.
        if (context.visibility == "PUBLIC") {
            val recipients = mutableSetOf(context.requesterId)
            recipients.addAll(context.recipientIds)
            recipients.forEach { recipientId ->
                sendToSubscriber(recipientId, deliveryMessage)
            }
        // PRIVATE: raspunsul merge exclusiv la initiator.
        } else if (context.visibility == "PRIVATE") {
            sendToSubscriber(context.requesterId, deliveryMessage)
        }

        // Curatare context:
        // - one-to-one: inchidem imediat dupa primul raspuns;
        // - one-to-many: inchidem cand au raspuns toti destinatarii.
        val shouldRemove = synchronized(stateLock) {
            val currentContext = questionContexts[requestId] ?: return@synchronized false
            currentContext.responders.add(senderId)
            currentContext.visibility == "PRIVATE" ||
                currentContext.responders.containsAll(currentContext.recipientIds)
        }

        if (shouldRemove) {
            synchronized(stateLock) {
                questionContexts.remove(requestId)
            }
        }
    }

    private fun handleMessage(subscriberId: String, receivedMessage: String) {
        // Protocol intern nou:
        // SUBSCRIBE|id|role
        // QUESTION|requestId|fromId|targetType|targetValue|base64Question
        // ANSWER|requestId|fromId|base64Answer
        when {
            receivedMessage.startsWith("QUESTION|") -> {
                // QUESTION|requestId|fromId|targetType|targetValue|base64Question
                val parts = receivedMessage.split("|", limit = 6)
                if (parts.size == 6) {
                    val requestId = parts[1]
                    val targetType = parts[3]
                    val targetValue = parts[4]
                    val questionBodyBase64 = parts[5]
                    handleQuestion(
                        senderId = subscriberId,
                        requestId = requestId,
                        targetType = targetType,
                        targetValue = targetValue,
                        questionBodyBase64 = questionBodyBase64
                    )
                }
            }
            receivedMessage.startsWith("ANSWER|") -> {
                // ANSWER|requestId|fromId|base64Answer
                val parts = receivedMessage.split("|", limit = 4)
                if (parts.size == 4) {
                    val requestId = parts[1]
                    val answerBodyBase64 = parts[3]
                    handleAnswer(
                        senderId = subscriberId,
                        requestId = requestId,
                        answerBodyBase64 = answerBodyBase64
                    )
                }
            }
        }
    }

    private fun handleSubscriberConnection(clientConnection: Socket) {
        val connectionId = "${clientConnection.inetAddress.hostAddress}:${clientConnection.port}"
        println("Conexiune noua: $connectionId")

        val bufferReader = BufferedReader(InputStreamReader(clientConnection.inputStream))
        var subscriberId: String? = null

        try {
            // Prima linie de pe socket trebuie sa fie SUBSCRIBE, altfel inchidem conexiunea.
            val subscribeMessage = bufferReader.readLine() ?: return
            val parts = subscribeMessage.split("|", limit = 3)
            if (parts.size != 3 || parts[0] != "SUBSCRIBE") {
                println("Mesaj de subscribe invalid de la $connectionId: $subscribeMessage")
                return
            }

            subscriberId = parts[1].trim()
            val role = parts[2].trim().uppercase()

            registerSubscriber(
                id = subscriberId,
                role = role,
                socket = clientConnection,
                connectionPort = clientConnection.port
            )

            println("Subscriber conectat: id=$subscriberId, role=$role, conn=$connectionId")

            while (true) {
                val receivedMessage = bufferReader.readLine() ?: break
                println("Primit mesaj de la $subscriberId: $receivedMessage")
                handleMessage(subscriberId, receivedMessage)
            }
        } finally {
            bufferReader.close()
            clientConnection.close()
            unregisterSubscriber(clientConnection.port)
        }
    }

    public fun run() {
        // se porneste un socket server TCP pe portul 1500 care asculta pentru conexiuni
        messageManagerSocket = ServerSocket(MESSAGE_MANAGER_PORT)
        println("MessageManagerMicroservice se executa pe portul: ${messageManagerSocket.localPort}")
        println("Se asteapta conexiuni si mesaje...")

        while (true) {
            // se asteapta conexiuni din partea clientilor subscriberi
            val clientConnection = messageManagerSocket.accept()

            // se porneste un thread separat pentru tratarea conexiunii cu clientul
            thread {
                handleSubscriberConnection(clientConnection)
            }
        }
    }
}

fun main() {
    val messageManagerMicroservice = MessageManagerMicroservice()
    messageManagerMicroservice.run()
}
