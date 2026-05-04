package com.sd.laborator

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket
import java.util.Base64
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.system.exitProcess

// Starea unei intrebari initiate local de student pana la timeout/raspuns.
data class StudentPendingRequest(
    var visibility: String = "UNKNOWN",
    val responses: MutableList<String> = mutableListOf()
)

class StudentMicroservice {
    private lateinit var questionDatabase: MutableList<Pair<String, String>>
    private lateinit var messageManagerSocket: Socket
    private lateinit var studentServerSocket: ServerSocket
    private val pendingLock = Object()
    // Corelare requestId -> starea intrebarii, pentru fluxul asincron prin manager.
    private val pendingRequests = hashMapOf<String, StudentPendingRequest>()
    // Socketul catre manager e partajat intre thread-uri, deci scriere sincronizata.
    private val managerWriteLock = Any()

    init {
        questionDatabase = loadQuestionDatabase()
    }

    companion object Constants {
        val MESSAGE_MANAGER_HOST = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"
        val MESSAGE_MANAGER_PORT = System.getenv("MESSAGE_MANAGER_PORT")?.toIntOrNull() ?: 1500
        val STUDENT_PORT = System.getenv("STUDENT_PORT")?.toIntOrNull() ?: 1700
        val RESPONSE_TIMEOUT_MS = System.getenv("RESPONSE_TIMEOUT_MS")?.toLongOrNull() ?: 3000L
        val SERVICE_ID = System.getenv("STUDENT_ID")
            ?: "student-${UUID.randomUUID().toString().substring(0, 8)}"
        val QUESTIONS_DATABASE_PATH = System.getenv("QUESTIONS_DATABASE_PATH") ?: "questions_database.txt"
    }

    private fun loadQuestionDatabase(): MutableList<Pair<String, String>> {
        val file = File(QUESTIONS_DATABASE_PATH)
        if (!file.exists()) {
            println("Fisierul de intrebari nu exista: $QUESTIONS_DATABASE_PATH")
            return mutableListOf()
        }

        val databaseLines: List<String> = file.readLines()
        val database = mutableListOf<Pair<String, String>>()
        for (i in 0 until databaseLines.size step 2) {
            if (i + 1 < databaseLines.size) {
                database.add(Pair(databaseLines[i], databaseLines[i + 1]))
            }
        }
        return database
    }

    private fun subscribeToMessageManager() {
        try {
            messageManagerSocket = Socket(MESSAGE_MANAGER_HOST, MESSAGE_MANAGER_PORT)
            println("M-am conectat la MessageManager!")
            // Inregistrare explicita in manager cu id logic studentX.
            sendToMessageManager("SUBSCRIBE|$SERVICE_ID|STUDENT")
        } catch (e: Exception) {
            println("Nu ma pot conecta la MessageManager!")
            exitProcess(1)
        }
    }

    private fun encodeMessage(message: String): String =
        Base64.getEncoder().encodeToString(message.toByteArray(Charsets.UTF_8))

    private fun decodeMessage(base64Message: String): String {
        return try {
            String(Base64.getDecoder().decode(base64Message), Charsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            ""
        }
    }

    private fun sendToMessageManager(message: String) {
        synchronized(managerWriteLock) {
            messageManagerSocket.getOutputStream().write((message + "\n").toByteArray())
        }
    }

    private fun respondToQuestion(question: String): String? {
        questionDatabase.forEach {
            if (it.first == question) {
                return it.second
            }
        }
        return null
    }

    private fun handleMessageManagerMessage(message: String) {
        // Student are doua roluri:
        // 1) responder la intrebari pe baza "bazei de date" locale
        // 2) initiator de intrebari catre teacher/all_students/studentX
        when {
            message.startsWith("QUESTION_DELIVERY|") -> {
                // QUESTION_DELIVERY|requestId|fromId|base64Question
                val parts = message.split("|", limit = 4)
                if (parts.size == 4) {
                    val requestId = parts[1]
                    val fromId = parts[2]
                    val questionBody = decodeMessage(parts[3])
                    println("Am primit intrebare de la $fromId: \"$questionBody\"")

                    val answer = respondToQuestion(questionBody)
                    if (answer != null) {
                        sendToMessageManager("ANSWER|$requestId|$SERVICE_ID|${encodeMessage(answer)}")
                    }
                }
            }
            message.startsWith("QUESTION_STATUS|") -> {
                // QUESTION_STATUS|requestId|visibility|recipientCount
                // Vizibilitatea e calculata central de manager in functie de routing.
                val parts = message.split("|", limit = 4)
                if (parts.size == 4) {
                    val requestId = parts[1]
                    val visibility = parts[2]
                    synchronized(pendingLock) {
                        pendingRequests[requestId]?.visibility = visibility
                        pendingLock.notifyAll()
                    }
                }
            }
            message.startsWith("ANSWER_DELIVERY|") -> {
                // ANSWER_DELIVERY|requestId|fromId|visibility|base64Answer
                val parts = message.split("|", limit = 5)
                if (parts.size == 5) {
                    val requestId = parts[1]
                    val fromId = parts[2]
                    val visibility = parts[3]
                    val answer = decodeMessage(parts[4])
                    // Prefix [PRIVATE]/[PUBLIC] pentru a reflecta tipul de comunicare.
                    val formattedAnswer = "[$visibility] $fromId: $answer"

                    var wasPending = false
                    synchronized(pendingLock) {
                        val pendingRequest = pendingRequests[requestId]
                        if (pendingRequest != null) {
                            pendingRequest.responses.add(formattedAnswer)
                            wasPending = true
                        }
                        pendingLock.notifyAll()
                    }

                    if (!wasPending) {
                        println("Raspuns primit (fara cerere activa): $formattedAnswer")
                    }
                }
            }
        }
    }

    private fun startMessageManagerListener() {
        thread(isDaemon = true) {
            val bufferReader = BufferedReader(InputStreamReader(messageManagerSocket.inputStream))
            while (true) {
                val message = bufferReader.readLine() ?: break
                handleMessageManagerMessage(message)
            }
            println("Conexiunea cu MessageManager a fost inchisa.")
        }
    }

    private fun parseClientRequest(requestLine: String): Triple<String, String, String> {
        if (!requestLine.startsWith("ASK|")) {
            // compatibilitate cu versiunea initiala (student -> toti studentii)
            return Triple("ALL_STUDENTS", "-", requestLine)
        }

        val parts = requestLine.split("|", limit = 4)
        if (parts.size != 4) {
            return Triple("ALL_STUDENTS", "-", requestLine)
        }

        return Triple(
            parts[1].trim().uppercase(),
            parts[2].trim(),
            parts[3].trim()
        )
    }

    private fun submitQuestion(targetType: String, targetValue: String, questionBody: String): String {
        // requestId unic pentru corelarea statusului si raspunsurilor.
        val requestId = UUID.randomUUID().toString()
        synchronized(pendingLock) {
            pendingRequests[requestId] = StudentPendingRequest()
        }

        sendToMessageManager(
            "QUESTION|$requestId|$SERVICE_ID|$targetType|$targetValue|${encodeMessage(questionBody)}"
        )

        val deadline = System.currentTimeMillis() + RESPONSE_TIMEOUT_MS
        val requestOutcome = synchronized(pendingLock) {
            // Asteptare pe evenimentele primite in listener-ul de manager.
            while (System.currentTimeMillis() < deadline) {
                val pendingRequest = pendingRequests[requestId] ?: break

                // Nu exista destinatari validi in acel moment.
                if (pendingRequest.visibility == "NONE") {
                    break
                }
                // Pentru one-to-one inchidem la primul raspuns.
                if (pendingRequest.visibility == "PRIVATE" && pendingRequest.responses.isNotEmpty()) {
                    break
                }

                val waitDuration = deadline - System.currentTimeMillis()
                if (waitDuration <= 0) {
                    break
                }
                pendingLock.wait(minOf(waitDuration, 200L))
            }

            pendingRequests.remove(requestId)
        }

        return when {
            requestOutcome == null -> "Eroare interna la procesarea intrebarii."
            requestOutcome.visibility == "NONE" ->
                "Nu exista destinatari disponibili pentru tipul de intrebare cerut."
            requestOutcome.responses.isEmpty() ->
                "Nu a raspuns nimeni la intrebare."
            // Pentru one-to-many poate exista un set de raspunsuri.
            else -> requestOutcome.responses.joinToString("\n")
        }
    }

    public fun run() {
        // Studentul se conecteaza la manager (publish/subscribe)...
        subscribeToMessageManager()
        // ...si, separat, expune propriul endpoint TCP pentru clienti.
        startMessageManagerListener()

        studentServerSocket = ServerSocket(STUDENT_PORT)
        println("StudentMicroservice [$SERVICE_ID] ruleaza pe portul: ${studentServerSocket.localPort}")
        println("Se asteapta mesaje de la MessageManager si cereri de la clienti...")

        while (true) {
            val clientConnection = studentServerSocket.accept()
            thread {
                val clientBufferReader = BufferedReader(InputStreamReader(clientConnection.inputStream))
                val requestLine = clientBufferReader.readLine()
                if (requestLine == null) {
                    clientConnection.close()
                    return@thread
                }

                val (targetType, targetValue, questionBody) = parseClientRequest(requestLine)
                val response = submitQuestion(targetType, targetValue, questionBody)
                clientConnection.getOutputStream().write((response + "\n").toByteArray())
                clientConnection.close()
            }
        }
    }
}

fun main() {
    val studentMicroservice = StudentMicroservice()
    studentMicroservice.run()
}
