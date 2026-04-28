import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

/**
 * MessageProcessorMicroservice
 *
 * Lab Task 2  — filters duplicate messages via .filter() on the reactive stream.
 * Lab Task 3  — sorts messages by timestamp in onComplete{} before forwarding.
 * Homework 1  — execution journal + REAL state persistence + crash recovery:
 *               messages are saved to logs/MessageProcessor_state.dat after each one
 *               is received; on startup if CYCLE_START > CYCLE_END the state file is
 *               loaded, the interrupted cycle is completed, then normal operation resumes.
 * Homework 2  — HeartBeat client registration.
 * Homework 3  — MetricsCollector instrumentation (local + master log).
 */
class MessageProcessorMicroservice {
    private var messageProcessorSocket: ServerSocket
    private lateinit var biddingProcessorSocket: Socket
    private var auctioneerConnection: Socket
    private var receiveInQueueObservable: Observable<String>
    private val subscriptions = CompositeDisposable()
    private val messageQueue: Queue<Message> = LinkedList<Message>()

    private val logWriter: PrintWriter
    private val metrics: MetricsCollector
    private val heartBeatClient: HeartBeatClient
    private val startTime = System.currentTimeMillis()

    // State persistence paths
    private val STATE_FILE   = "logs/MessageProcessor_state.dat"
    private val JOURNAL_FILE = "logs/MessageProcessor_journal.log"

    companion object {
        const val MESSAGE_PROCESSOR_PORT = 1600
        const val BIDDING_PROCESSOR_HOST = "localhost"
        const val BIDDING_PROCESSOR_PORT = 1700
        val DATE_FMT = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
    }

    // ── Journal ───────────────────────────────────────────────────────────────

    private fun logEvent(event: String) {
        val ts = DATE_FMT.format(Date())
        val line = "[$ts] $event"
        println("[JOURNAL] $line")
        logWriter.println(line)
        logWriter.flush()
    }

    // ── State Persistence ─────────────────────────────────────────────────────

    /**
     * Appends one serialized Message to the state file.
     * Called for every message received so the queue survives a crash.
     */
    private fun persistMessage(msg: Message) {
        try {
            java.io.File(STATE_FILE).appendText(String(msg.serialize()))
        } catch (e: Exception) {
            logEvent("WARNING: Failed to persist message to state file: $e")
        }
    }

    /**
     * Loads all messages from the state file into messageQueue.
     * Each line in the file is one serialized message.
     */
    private fun loadStateFile(): List<Message> {
        val stateFile = java.io.File(STATE_FILE)
        if (!stateFile.exists()) return emptyList()
        val loaded = mutableListOf<Message>()
        stateFile.readLines().forEach { line ->
            if (line.isNotBlank()) {
                try {
                    loaded.add(Message.deserialize(line.toByteArray()))
                } catch (e: Exception) {
                    logEvent("WARNING: Could not deserialize state line: '$line': $e")
                }
            }
        }
        return loaded
    }

    /** Deletes the state file once a cycle completes successfully. */
    private fun clearStateFile() {
        runCatching { java.io.File(STATE_FILE).delete() }
    }

    // ── Journal + Recovery ────────────────────────────────────────────────────

    /**
     * Checks the journal for an interrupted cycle (CYCLE_START without a matching
     * CYCLE_END). If found, loads the persisted queue and finishes the interrupted
     * cycle by forwarding the data directly to BiddingProcessor — then returns true
     * so the caller knows recovery was performed and normal operation can resume.
     */
    private fun checkAndRecoverFromJournal(): Boolean {
        val journalFile = java.io.File(JOURNAL_FILE)
        if (!journalFile.exists()) return false

        val lines = journalFile.readLines()
        var lastStart = -1; var lastEnd = -1
        lines.forEachIndexed { idx, line ->
            if (line.contains("CYCLE_START")) lastStart = idx
            if (line.contains("CYCLE_END"))   lastEnd   = idx
        }

        if (lastStart <= lastEnd) return false   // no interrupted cycle

        logEvent("RECOVERY: Interrupted cycle detected (journal line $lastStart). Loading persisted state...")
        metrics.record("recovery_detected", mapOf("journal_line" to lastStart.toString()))

        val recovered = loadStateFile()
        if (recovered.isEmpty()) {
            logEvent("RECOVERY: State file empty or missing — nothing to replay. Continuing fresh.")
            metrics.record("recovery_empty")
            return true
        }

        logEvent("RECOVERY: Loaded ${recovered.size} messages from state file. Completing interrupted cycle...")
        metrics.record("recovery_loaded", mapOf("messages" to recovered.size.toString()))

        recovered.forEach { messageQueue.add(it) }

        // Sort (Lab Task 3) and forward the recovered messages
        val sorted = messageQueue.toList().sortedBy { it.timestamp }
        messageQueue.clear()
        sorted.forEach { messageQueue.add(it) }

        sendProcessedMessages()   // completes the interrupted cycle

        // After forwarding, clear state and start fresh
        clearStateFile()
        messageQueue.clear()
        logEvent("RECOVERY: Interrupted cycle completed. Resuming normal operation.")
        metrics.record("recovery_complete", mapOf("forwarded" to sorted.size.toString()))
        return true
    }

    private fun shutdown() {
        logEvent("Shutdown initiated.")
        metrics.stop()
        heartBeatClient.stop()
        subscriptions.dispose()
        runCatching { messageProcessorSocket.close() }
        runCatching { logWriter.close() }
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        java.io.File("logs").mkdirs()
        logWriter = PrintWriter(BufferedWriter(FileWriter(JOURNAL_FILE, true)))
        metrics   = MetricsCollector("MessageProcessorMicroservice")

        logEvent("MessageProcessorMicroservice starting up.")

        // HeartBeat (Homework 2) — optional; won't crash if HB service is absent
        heartBeatClient = HeartBeatClient("MessageProcessorMicroservice")
        heartBeatClient.start()

        // CTRL+C shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread { shutdown() })

        messageProcessorSocket = ServerSocket(MESSAGE_PROCESSOR_PORT)
        println("MessageProcessorMicroservice se executa pe portul: ${messageProcessorSocket.localPort}")
        println("Se asteapta mesaje pentru procesare...")
        println("Apasati CTRL+C pentru a opri.")
        logEvent("Listening on port $MESSAGE_PROCESSOR_PORT.")

        // ── Homework 1: crash recovery ─────────────────────────────────────────
        // This must happen BEFORE we wait for Auctioneer, so any recovered data is
        // forwarded to BiddingProcessor before a new auction cycle begins.
        checkAndRecoverFromJournal()

        // Wait for AuctioneerMicroservice to connect
        auctioneerConnection = messageProcessorSocket.accept()
        logEvent("CYCLE_START: Auctioneer connected from ${auctioneerConnection.remoteSocketAddress}.")
        metrics.record("cycle_start", mapOf("auctioneer" to auctioneerConnection.remoteSocketAddress.toString()))

        val bufferReader = BufferedReader(InputStreamReader(auctioneerConnection.inputStream))

        receiveInQueueObservable = Observable.create<String> { emitter ->
            while (true) {
                val line = bufferReader.readLine()
                if (line == null) {
                    bufferReader.close()
                    auctioneerConnection.close()
                    logEvent("ERROR: Auctioneer disconnected unexpectedly.")
                    emitter.onError(Exception("AuctioneerMicroservice disconnected."))
                    break
                }
                if (Message.deserialize(line.toByteArray()).body == "final") {
                    emitter.onComplete()
                    break
                } else {
                    emitter.onNext(line)
                }
            }
        }
    }

    // ── Processing ────────────────────────────────────────────────────────────

    private fun receiveAndProcessMessages() {
        logEvent("Starting to receive and process messages.")
        val seenKeys = mutableSetOf<String>()

        val sub = receiveInQueueObservable
            // ── Lab Task 2: Filter duplicates ──────────────────────────────────
            .filter { raw ->
                val msg = Message.deserialize(raw.toByteArray())
                val key = msg.deduplicationKey()
                if (key in seenKeys) {
                    logEvent("DUPLICATE filtered: key='$key'")
                    metrics.record("duplicate_filtered", mapOf("key" to key))
                    false
                } else {
                    seenKeys.add(key)
                    true
                }
            }
            .subscribeBy(
                onNext = { raw ->
                    val msg = Message.deserialize(raw.toByteArray())
                    println(msg)
                    logEvent("Received: $msg")
                    metrics.record("message_received", mapOf("sender" to msg.sender, "body" to msg.body))
                    messageQueue.add(msg)
                    // ── Persist every message so we can recover on crash ────────
                    persistMessage(msg)
                },
                onComplete = {
                    println("S-au primit toate mesajele.")
                    logEvent("All messages received. Unique: ${messageQueue.size}")
                    metrics.record("all_received", mapOf("count" to messageQueue.size.toString()))

                    // ── Lab Task 3: Sort by timestamp ───────────────────────────
                    val sorted = messageQueue.toList().sortedBy { it.timestamp }
                    messageQueue.clear()
                    sorted.forEach { messageQueue.add(it) }
                    logEvent("Sorted ${sorted.size} messages by timestamp.")
                    metrics.record("sorted", mapOf("count" to sorted.size.toString()))

                    // Acknowledge to Auctioneer
                    val ack = Message.create(
                        "${auctioneerConnection.localAddress}:${auctioneerConnection.localPort}",
                        "am primit tot"
                    )
                    auctioneerConnection.getOutputStream().write(ack.serialize())
                    auctioneerConnection.close()
                    logEvent("Acknowledged receipt to Auctioneer.")

                    sendProcessedMessages()
                },
                onError = { err ->
                    println("Eroare: $err")
                    logEvent("ERROR: $err")
                    // State file remains on disk for recovery next startup
                }
            )
        subscriptions.add(sub)
    }

    internal fun sendProcessedMessages() {
        try {
            biddingProcessorSocket = Socket(BIDDING_PROCESSOR_HOST, BIDDING_PROCESSOR_PORT)
            logEvent("Connected to BiddingProcessor. Sending ${messageQueue.size} messages.")

            println("Trimit urmatoarele mesaje:")
            Observable.fromIterable(messageQueue).subscribeBy(
                onNext = { msg ->
                    println(msg.toString())
                    logEvent("Sending: $msg")
                    biddingProcessorSocket.getOutputStream().write(msg.serialize())
                },
                onComplete = {
                    val finalMsg = Message.create(
                        "${biddingProcessorSocket.localAddress}:${biddingProcessorSocket.localPort}",
                        "final"
                    )
                    biddingProcessorSocket.getOutputStream().write(finalMsg.serialize())
                    biddingProcessorSocket.close()

                    val elapsed = System.currentTimeMillis() - startTime
                    logEvent("CYCLE_END: All messages forwarded. Elapsed: ${elapsed}ms.")
                    metrics.record("cycle_end", mapOf("elapsed_ms" to elapsed.toString()))

                    // Clear state file — cycle completed successfully
                    clearStateFile()

                    metrics.stop()
                    heartBeatClient.stop()
                    subscriptions.dispose()
                    logWriter.close()
                }
            )
        } catch (e: Exception) {
            println("Nu ma pot conecta la BiddingProcessor!")
            logEvent("FATAL: Cannot connect to BiddingProcessor: $e")
            // State file intentionally NOT deleted — recovery can replay on next startup
            exitProcess(1)
        }
    }

    fun run() {
        receiveAndProcessMessages()
    }
}

fun main(args: Array<String>) {
    MessageProcessorMicroservice().run()
}