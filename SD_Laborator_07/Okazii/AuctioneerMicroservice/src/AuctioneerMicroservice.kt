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
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

/**
 * AuctioneerMicroservice
 *
 * Central coordination: accepts bids for AUCTION_DURATION ms, forwards to
 * MessageProcessor, and announces the winner result to all bidders.
 *
 * Homework 1 — execution journal + REAL state persistence + crash recovery:
 *   Each accepted bid is appended to logs/Auctioneer_state.dat.
 *   On startup, if CYCLE_START > CYCLE_END, the persisted queue is loaded
 *   and forwarded to MessageProcessor to complete the interrupted cycle.
 *   NOTE: Bidder socket connections cannot be persisted across a crash, so the
 *   result notification phase cannot be replayed — only the forwarding phase.
 * Homework 2 — HeartBeat client registration.
 * Homework 3 — MetricsCollector instrumentation (local + master log).
 */
class AuctioneerMicroservice {
    private var auctioneerSocket: ServerSocket
    private lateinit var messageProcessorSocket: Socket
    private var receiveBidsObservable: Observable<String>
    private val subscriptions = CompositeDisposable()
    private val bidQueue: Queue<Message> = LinkedList<Message>()
    private val bidderConnections: MutableList<Socket> = mutableListOf()

    private var totalBidsReceived = 0
    private val auctionStartTime = System.currentTimeMillis()

    private val logWriter: PrintWriter
    private val metrics: MetricsCollector
    private val heartBeatClient: HeartBeatClient

    private val STATE_FILE   = "logs/Auctioneer_state.dat"
    private val JOURNAL_FILE = "logs/Auctioneer_journal.log"

    companion object {
        const val MESSAGE_PROCESSOR_HOST = "localhost"
        const val MESSAGE_PROCESSOR_PORT = 1600
        const val AUCTIONEER_PORT        = 1500
        const val AUCTION_DURATION: Long = 15_000
        val DATE_FMT = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
    }

    // ── Journal ───────────────────────────────────────────────────────────────

    private fun logEvent(event: String) {
        val ts = DATE_FMT.format(Date())
        val line = "[$ts] $event"
        println("[JOURNAL] $line")
        logWriter.println(line)
        logWriter.flush()
        MetricsCollector.appendToMasterLog("[$ts] [AuctioneerMicroservice] JOURNAL $event")
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
                try { loaded.add(Message.deserialize(line.toByteArray())) }
                catch (e: Exception) { logEvent("WARNING: Cannot deserialize state line: $e") }
            }
        }
        return loaded
    }

    private fun clearStateFile() { runCatching { java.io.File(STATE_FILE).delete() } }

    // ── Journal + Recovery ────────────────────────────────────────────────────

    /**
     * Detects interrupted cycle and, if found, loads the persisted bid queue
     * and forwards it to MessageProcessor to complete the cycle.
     * Bidder sockets are not recoverable across crashes — the result notification
     * is skipped and a RECOVERY_PARTIAL log entry is written instead.
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

        logEvent("RECOVERY: Loaded ${recovered.size} bids. Forwarding to MessageProcessor...")
        metrics.record("recovery_loaded", mapOf("bids" to recovered.size.toString()))
        recovered.forEach { bidQueue.add(it) }

        // Forward the recovered bids to MessageProcessor
        forwardBids(isRecovery = true)

        clearStateFile()
        bidQueue.clear()
        logEvent("RECOVERY: Interrupted cycle forwarded. NOTE: Bidder result notifications skipped (sockets not recoverable).")
        metrics.record("recovery_complete", mapOf("forwarded" to recovered.size.toString()))
        return true
    }

    private fun shutdown() {
        logEvent("Shutdown initiated.")
        metrics.record("shutdown", mapOf("uptime_ms" to (System.currentTimeMillis() - auctionStartTime).toString()))
        metrics.stop()
        heartBeatClient.stop()
        subscriptions.dispose()
        runCatching { auctioneerSocket.close() }
        runCatching { logWriter.close() }
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        java.io.File("logs").mkdirs()
        logWriter = PrintWriter(BufferedWriter(FileWriter(JOURNAL_FILE, true)))
        metrics   = MetricsCollector("AuctioneerMicroservice")

        logEvent("AuctioneerMicroservice starting up.")

        heartBeatClient = HeartBeatClient("AuctioneerMicroservice")
        heartBeatClient.start()

        Runtime.getRuntime().addShutdownHook(Thread { shutdown() })

        auctioneerSocket = ServerSocket(AUCTIONEER_PORT)
        auctioneerSocket.setSoTimeout(AUCTION_DURATION.toInt())
        println("AuctioneerMicroservice se executa pe portul: ${auctioneerSocket.localPort}")
        println("Se asteapta oferte de la bidderi... (${AUCTION_DURATION / 1000}s)")
        println("Apasati CTRL+C pentru a opri.")
        metrics.record("startup", mapOf("port" to AUCTIONEER_PORT.toString(), "duration_ms" to AUCTION_DURATION.toString()))

        // ── Homework 1: crash recovery ─────────────────────────────────────────
        // Verificam jurnalul inainte sa scriem CYCLE_START pentru ciclul nou.
        // Altfel, fiecare pornire curata ar fi interpretata ca un crash neterminat.
        checkAndRecoverFromJournal()

        logEvent("CYCLE_START: Listening on port $AUCTIONEER_PORT. Duration: ${AUCTION_DURATION}ms.")
        metrics.record("cycle_start", mapOf("port" to AUCTIONEER_PORT.toString()))

        receiveBidsObservable = Observable.create<String> { emitter ->
            while (true) {
                try {
                    val conn = auctioneerSocket.accept()
                    bidderConnections.add(conn)
                    conn.soTimeout = 2_000
                    logEvent("Bidder connected: ${conn.remoteSocketAddress}. Total: ${bidderConnections.size}")
                    metrics.record("bidder_connected", mapOf(
                        "remote" to conn.remoteSocketAddress.toString(),
                        "total"  to bidderConnections.size.toString()
                    ))

                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    var linesFromBidder = 0

                    // Un Bidder poate trimite doua linii identice pentru testul de duplicate.
                    // Citim pana la EOF/timeout, dar pastram socketul deschis ca sa trimitem rezultatul.
                    while (true) {
                        val line = try {
                            reader.readLine()
                        } catch (e: SocketTimeoutException) {
                            logEvent("No more bid lines from ${conn.remoteSocketAddress} after ${linesFromBidder} line(s).")
                            break
                        }

                        if (line == null) break
                        if (line.isBlank()) continue

                        totalBidsReceived++
                        linesFromBidder++
                        emitter.onNext(line)
                    }

                    if (linesFromBidder == 0) {
                        logEvent("WARNING: Bidder ${conn.remoteSocketAddress} disconnected without sending a bid.")
                    }

                } catch (e: SocketTimeoutException) {
                    emitter.onComplete()
                    break
                }
            }
        }
    }

    // ── Core Logic ────────────────────────────────────────────────────────────

    private fun receiveBids() {
        subscriptions.add(receiveBidsObservable.subscribeBy(
            onNext = { raw ->
                val msg = Message.deserialize(raw.toByteArray())
                println(msg)
                logEvent("Bid received: $msg")
                metrics.record("bid_received", mapOf("sender" to msg.sender, "body" to msg.body))
                bidQueue.add(msg)
                // ── Persist every bid for crash recovery ──────────────────────
                persistBid(msg)
            },
            onComplete = {
                val elapsed = System.currentTimeMillis() - auctionStartTime
                println("Licitatia s-a incheiat! Se trimit ofertele spre procesare...")
                logEvent("Auction closed after ${elapsed}ms. Bids: ${bidQueue.size}.")
                metrics.record("auction_closed", mapOf("duration_ms" to elapsed.toString(), "bids" to bidQueue.size.toString()))
                forwardBids(isRecovery = false)
            },
            onError = { err ->
                println("Eroare: $err")
                logEvent("ERROR: $err")
            }
        ))
    }

    private fun forwardBids(isRecovery: Boolean = false) {
        try {
            messageProcessorSocket = Socket(MESSAGE_PROCESSOR_HOST, MESSAGE_PROCESSOR_PORT)
            logEvent("Connected to MessageProcessor. Forwarding ${bidQueue.size} bids${if (isRecovery) " (RECOVERY)" else ""}.")

            subscriptions.add(Observable.fromIterable(bidQueue).subscribeBy(
                onNext = { msg ->
                    messageProcessorSocket.getOutputStream().write(msg.serialize())
                    println("Am trimis mesajul: $msg")
                    logEvent("Forwarded: $msg")
                },
                onComplete = {
                    println("Am trimis toate ofertele catre MessageProcessor.")
                    val finalMsg = Message.create(
                        "${messageProcessorSocket.localAddress}:${messageProcessorSocket.localPort}",
                        "final"
                    )
                    messageProcessorSocket.getOutputStream().write(finalMsg.serialize())
                    logEvent("Sent 'final' to MessageProcessor.")

                    // Wait for MessageProcessor acknowledgment
                    BufferedReader(InputStreamReader(messageProcessorSocket.inputStream)).readLine()
                    messageProcessorSocket.close()
                    logEvent("MessageProcessor acknowledged. ${if (isRecovery) "Recovery complete." else "Moving to finishAuction."}")

                    if (!isRecovery) finishAuction()
                }
            ))
        } catch (e: Exception) {
            println("Nu ma pot conecta la MessageProcessor!")
            logEvent("FATAL: Cannot connect to MessageProcessor: $e")
            exitProcess(1)
        }
    }

    private fun finishAuction() {
        try {
            val bpConn = auctioneerSocket.accept()
            val receivedMsg = BufferedReader(InputStreamReader(bpConn.inputStream)).readLine()

            val result = Message.deserialize(receivedMsg.toByteArray())
            val winningPrice = result.bidAmount()
            if (winningPrice == null) {
                logEvent("FATAL: Winner message has no valid bid amount: $result")
                exitProcess(1)
            }
            println("Rezultat: ${result.sender} a castigat cu pretul: $winningPrice")
            logEvent("Winner: sender=${result.sender}, price=$winningPrice")
            metrics.record("auction_result", mapOf("winner" to result.sender, "price" to winningPrice.toString()))

            val winMsg  = Message.create(auctioneerSocket.localSocketAddress.toString(),
                "Licitatie castigata! Pret castigator: $winningPrice")
            val loseMsg = Message.create(auctioneerSocket.localSocketAddress.toString(),
                "Licitatie pierduta...")

            bidderConnections.forEach { socket ->
                try {
                    if (socket.remoteSocketAddress.toString() == result.sender) {
                        socket.getOutputStream().write(winMsg.serialize())
                        logEvent("WIN -> ${socket.remoteSocketAddress}")
                    } else {
                        socket.getOutputStream().write(loseMsg.serialize())
                    }
                    socket.close()
                } catch (e: Exception) {
                    logEvent("WARNING: Could not notify ${socket.remoteSocketAddress}: $e")
                }
            }

            val totalElapsed = System.currentTimeMillis() - auctionStartTime
            logEvent("CYCLE_END: Complete. Duration: ${totalElapsed}ms. Notified: ${bidderConnections.size}.")
            metrics.record("cycle_end", mapOf(
                "duration_ms" to totalElapsed.toString(),
                "notified"    to bidderConnections.size.toString()
            ))

            // Cycle completed — state file can be cleared
            clearStateFile()

        } catch (e: Exception) {
            println("Eroare la finalizarea licitatiei: $e")
            logEvent("FATAL: finishAuction error: $e")
            exitProcess(1)
        }

        metrics.stop()
        heartBeatClient.stop()
        subscriptions.dispose()
        logWriter.close()
    }

    fun run() {
        receiveBids()
    }
}

fun main(args: Array<String>) {
    AuctioneerMicroservice().run()
}
