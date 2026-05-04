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
 * BiddingProcessorMicroservice
 *
 * Receives processed bids, decides the winner (highest bid), reports to Auctioneer.
 *
 * Homework 1 — execution journal + REAL state persistence + crash recovery:
 *   Each received bid is appended to logs/BiddingProcessor_state.dat.
 *   On startup, if CYCLE_START > CYCLE_END, the state file is loaded and
 *   the winner is decided + reported to Auctioneer to complete the interrupted cycle.
 * Homework 2  — HeartBeat client registration.
 * Homework 3  — MetricsCollector instrumentation (local + master log).
 */
class BiddingProcessorMicroservice {
    private var biddingProcessorSocket: ServerSocket
    private lateinit var auctioneerSocket: Socket
    private var receiveProcessedBidsObservable: Observable<String>
    private val subscriptions = CompositeDisposable()
    private val processedBidsQueue: Queue<Message> = LinkedList<Message>()

    private val startTime = System.currentTimeMillis()
    private val logWriter: PrintWriter
    private val metrics: MetricsCollector
    private val heartBeatClient: HeartBeatClient

    private val STATE_FILE   = "logs/BiddingProcessor_state.dat"
    private val JOURNAL_FILE = "logs/BiddingProcessor_journal.log"

    companion object {
        const val BIDDING_PROCESSOR_PORT = 1700
        const val AUCTIONEER_PORT        = 1500
        const val AUCTIONEER_HOST        = "localhost"
        val DATE_FMT = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
    }

    // ── Journal ───────────────────────────────────────────────────────────────

    private fun logEvent(event: String) {
        val ts = DATE_FMT.format(Date())
        val line = "[$ts] $event"
        println("[JOURNAL] $line")
        logWriter.println(line)
        logWriter.flush()
        MetricsCollector.appendToMasterLog("[$ts] [BiddingProcessorMicroservice] JOURNAL $event")
    }

    // ── State Persistence ─────────────────────────────────────────────────────

    private fun persistBid(msg: Message) {
        try {
            java.io.File(STATE_FILE).appendText(String(msg.serialize()))
        } catch (e: Exception) {
            logEvent("WARNING: Failed to persist bid: $e")
        }
    }

    private fun loadStateFile(): List<Message> {
        val f = java.io.File(STATE_FILE)
        if (!f.exists()) return emptyList()
        val loaded = mutableListOf<Message>()
        f.readLines().forEach { line ->
            if (line.isNotBlank()) {
                try {
                    loaded.add(Message.deserialize(line.toByteArray()))
                } catch (e: Exception) {
                    logEvent("WARNING: Cannot deserialize state line '$line': $e")
                }
            }
        }
        return loaded
    }

    private fun clearStateFile() {
        runCatching { java.io.File(STATE_FILE).delete() }
    }

    // ── Journal + Recovery ────────────────────────────────────────────────────

    /**
     * Detects an interrupted processing cycle and, if found, loads the persisted
     * bids, decides the winner, and reports it to AuctioneerMicroservice —
     * completing the interrupted cycle before normal operation resumes.
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
        if (lastStart <= lastEnd) return false

        logEvent("RECOVERY: Interrupted cycle at journal line $lastStart. Loading persisted bids...")
        metrics.record("recovery_detected", mapOf("journal_line" to lastStart.toString()))

        val recovered = loadStateFile()
        if (recovered.isEmpty()) {
            logEvent("RECOVERY: State file empty — nothing to replay. Continuing fresh.")
            metrics.record("recovery_empty")
            logEvent("CYCLE_END: Recovery finished with empty state.")
            return true
        }

        logEvent("RECOVERY: Loaded ${recovered.size} bids. Completing interrupted cycle...")
        metrics.record("recovery_loaded", mapOf("bids" to recovered.size.toString()))

        recovered.forEach { processedBidsQueue.add(it) }
        decideAndReportWinner(stopAfterReport = false)   // completes the interrupted cycle

        clearStateFile()
        processedBidsQueue.clear()
        logEvent("RECOVERY: Interrupted cycle completed. Resuming normal operation.")
        metrics.record("recovery_complete")
        return true
    }

    private fun shutdown() {
        logEvent("Shutdown initiated.")
        metrics.stop()
        heartBeatClient.stop()
        subscriptions.dispose()
        runCatching { biddingProcessorSocket.close() }
        runCatching { logWriter.close() }
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        java.io.File("logs").mkdirs()
        logWriter = PrintWriter(BufferedWriter(FileWriter(JOURNAL_FILE, true)))
        metrics   = MetricsCollector("BiddingProcessorMicroservice")

        logEvent("BiddingProcessorMicroservice starting up.")

        heartBeatClient = HeartBeatClient("BiddingProcessorMicroservice")
        heartBeatClient.start()

        Runtime.getRuntime().addShutdownHook(Thread { shutdown() })

        biddingProcessorSocket = ServerSocket(BIDDING_PROCESSOR_PORT)
        println("BiddingProcessorMicroservice se executa pe portul: ${biddingProcessorSocket.localPort}")
        println("Se asteapta ofertele pentru finalizarea licitatiei...")
        println("Apasati CTRL+C pentru a opri.")
        logEvent("Listening on port $BIDDING_PROCESSOR_PORT.")

        // ── Homework 1: crash recovery before accepting new connections ────────
        checkAndRecoverFromJournal()

        // Wait for MessageProcessorMicroservice
        val msgProcConn = biddingProcessorSocket.accept()
        val bufferReader = BufferedReader(InputStreamReader(msgProcConn.inputStream))
        logEvent("CYCLE_START: MessageProcessor connected from ${msgProcConn.remoteSocketAddress}.")
        metrics.record("cycle_start", mapOf("source" to msgProcConn.remoteSocketAddress.toString()))

        receiveProcessedBidsObservable = Observable.create<String> { emitter ->
            while (true) {
                val line = bufferReader.readLine()
                if (line == null) {
                    bufferReader.close()
                    msgProcConn.close()
                    logEvent("ERROR: MessageProcessor disconnected unexpectedly.")
                    emitter.onError(Exception("MessageProcessorMicroservice disconnected."))
                    break
                }
                if (Message.deserialize(line.toByteArray()).body == "final") {
                    emitter.onComplete()
                    // Acknowledge receipt
                    val ack = Message.create(
                        "${msgProcConn.localAddress}:${msgProcConn.localPort}",
                        "am primit tot"
                    )
                    msgProcConn.getOutputStream().write(ack.serialize())
                    msgProcConn.close()
                    logEvent("All bids received. Acknowledged MessageProcessor.")
                    break
                } else {
                    emitter.onNext(line)
                }
            }
        }
    }

    // ── Processing ────────────────────────────────────────────────────────────

    private fun receiveProcessedBids() {
        val sub = receiveProcessedBidsObservable.subscribeBy(
            onNext = { raw ->
                val msg = Message.deserialize(raw.toByteArray())
                println(msg)
                logEvent("Bid received: $msg")
                metrics.record("bid_received", mapOf("sender" to msg.sender, "body" to msg.body))
                processedBidsQueue.add(msg)
                // ── Persist every bid so recovery can replay ─────────────────
                persistBid(msg)
            },
            onComplete = {
                logEvent("All bids collected: ${processedBidsQueue.size}. Deciding winner.")
                metrics.record("all_bids_received", mapOf("count" to processedBidsQueue.size.toString()))
                decideAndReportWinner()
                clearStateFile()
            },
            onError = { err ->
                println("Eroare: $err")
                logEvent("ERROR: $err")
                // State file intentionally kept for recovery
            }
        )
        subscriptions.add(sub)
    }

    private fun decideAndReportWinner(stopAfterReport: Boolean = true) {
        val winner: Message? = processedBidsQueue.toList().maxByOrNull {
            it.bidAmount() ?: Int.MIN_VALUE
        }

        val price = winner?.bidAmount()
        if (winner == null || price == null) {
            logEvent("ERROR: No bids — cannot decide winner.")
            metrics.record("no_winner")
            return
        }

        val idStr = if (winner.hasIdentity()) "(${winner.name}, ${winner.phone}, ${winner.email})" else ""
        println("Castigatorul este: ${winner.sender} $idStr cu pretul $price")
        logEvent("Winner decided: sender=${winner.sender} $idStr, price=$price")
        metrics.record("winner_decided", mapOf(
            "sender" to winner.sender,
            "price"  to price.toString(),
            "name"   to winner.name,
            "phone"  to winner.phone,
            "email"  to winner.email
        ))

        try {
            auctioneerSocket = Socket(AUCTIONEER_HOST, AUCTIONEER_PORT)
            auctioneerSocket.getOutputStream().write(winner.serialize())
            auctioneerSocket.close()

            val elapsed = System.currentTimeMillis() - startTime
            println("Am anuntat castigatorul catre AuctioneerMicroservice.")
            logEvent("CYCLE_END: Winner sent. Total time: ${elapsed}ms.")
            metrics.record("cycle_end", mapOf("elapsed_ms" to elapsed.toString()))

        } catch (e: Exception) {
            println("Nu ma pot conecta la Auctioneer!")
            logEvent("FATAL: Cannot connect to Auctioneer: $e")
            exitProcess(1)
        }

        if (stopAfterReport) {
            metrics.stop()
            heartBeatClient.stop()
            subscriptions.dispose()
            logWriter.close()
        }
    }

    fun run() {
        receiveProcessedBids()
    }
}

fun main(args: Array<String>) {
    BiddingProcessorMicroservice().run()
}
