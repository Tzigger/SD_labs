import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random
import kotlin.system.exitProcess

/**
 * BidderMicroservice
 *
 * Models an auction participant with a personal identity (name, phone, email).
 * Connects to AuctioneerMicroservice, places a random bid (with occasional duplicate
 * to exercise the MessageProcessor deduplication logic), then waits for the result.
 *
 * - Homework 1: identity fields + per-instance execution journal with crash recovery.
 * - Homework 2: HeartBeat client registration.
 * - Graceful CTRL+C via JVM shutdown hook.
 */
class BidderMicroservice(private val configuredIndex: Int? = null) {
    private val auctioneerSocket: Socket
    private val auctionResultObservable: Observable<String>
    private var myIdentity: String = "[BIDDER_NECONECTAT]"

    private val bidderName:  String
    private val bidderPhone: String
    private val bidderEmail: String
    private val bidderInstanceId: String
    private val stateFilePath: String
    private var recoveredBidBody: String? = null

    private val logWriter: PrintWriter
    private val metrics: MetricsCollector
    private val heartBeatClient: HeartBeatClient

    companion object Constants {
        const val AUCTIONEER_HOST = "localhost"
        const val AUCTIONEER_PORT = 1500
        const val MAX_BID = 10_000
        const val MIN_BID = 1_000
        val DATE_FMT = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")

        private val NAMES  = listOf("Ion Popescu", "Maria Ionescu", "Andrei Popa", "Elena Radu",
                                    "Mihai Constantin", "Ana Dima", "Radu Gheorghe", "Ioana Stan",
                                    "Cristian Munteanu", "Lucia Petrescu")
        private val PHONES = listOf("0740111222", "0720333444", "0760555666", "0730777888",
                                    "0750999000", "0740123456", "0720654321", "0760112233",
                                    "0730998877", "0750665544")
        private val EMAILS = listOf("ion@mail.ro", "maria@mail.ro", "andrei@mail.ro", "elena@mail.ro",
                                    "mihai@mail.ro", "ana@mail.ro", "radu@mail.ro", "ioana@mail.ro",
                                    "cristian@mail.ro", "lucia@mail.ro")
    }

    // ── Journal helpers ───────────────────────────────────────────────────────

    private fun logEvent(event: String) {
        val ts = DATE_FMT.format(Date())
        val line = "[$ts] $myIdentity $event"
        println("[JOURNAL] $line")
        logWriter.println(line)
        logWriter.flush()
        MetricsCollector.appendToMasterLog("[$ts] [BidderMicroservice:$bidderInstanceId] JOURNAL $event")
    }

    private fun checkAndRecoverFromJournal(instanceId: String): Boolean {
        val f = java.io.File("logs/Bidder_${instanceId}_journal.log")
        if (!f.exists()) return false
        val lines = f.readLines()
        var lastStart = -1; var lastEnd = -1
        lines.forEachIndexed { idx, line ->
            if (line.contains("CYCLE_START")) lastStart = idx
            if (line.contains("CYCLE_END"))   lastEnd   = idx
        }
        return lastStart > lastEnd
    }

    private fun persistBidForRecovery(msg: Message) {
        // Salvam ultima oferta locala. Daca bidderul cade dupa trimitere,
        // la repornire refacem oferta cu noul socket, dar cu acelasi pret/date.
        runCatching { java.io.File(stateFilePath).writeText(String(msg.serialize())) }
            .onFailure { logEvent("WARNING: Nu pot salva state-ul bidderului: $it") }
    }

    private fun loadRecoveredBid(): Message? {
        val f = java.io.File(stateFilePath)
        if (!f.exists()) return null
        return runCatching {
            Message.deserialize(f.readText().trim().toByteArray())
        }.getOrNull()
    }

    private fun clearRecoveredBid() {
        runCatching { java.io.File(stateFilePath).delete() }
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        val envIndex = System.getenv("BIDDER_INDEX")?.toIntOrNull()
        val stableIndex = configuredIndex ?: envIndex

        // Daca scriptul Bash ne trimite indexul bidderului, generam date stabile.
        // Asa logurile/recovery-ul sunt usor de urmarit pentru 100 de procese.
        if (stableIndex != null) {
            bidderName = "Bidder $stableIndex"
            bidderPhone = "07%08d".format(stableIndex)
            bidderEmail = "bidder$stableIndex@mail.ro"
        } else {
            val idx = Random.nextInt(NAMES.size)
            bidderName  = NAMES[idx]
            bidderPhone = PHONES[idx]
            bidderEmail = EMAILS[idx]
        }

        // Connect to Auctioneer — if this fails we exit immediately (logWriter not needed)
        try {
            auctioneerSocket = Socket(AUCTIONEER_HOST, AUCTIONEER_PORT)
        } catch (e: Exception) {
            System.err.println("[BIDDER] Nu ma pot conecta la Auctioneer: $e")
            exitProcess(1)
        }

        bidderInstanceId = stableIndex?.toString() ?: auctioneerSocket.localPort.toString()
        stateFilePath = "logs/Bidder_${bidderInstanceId}_state.dat"
        myIdentity = "[Bidder $bidderInstanceId:${auctioneerSocket.localPort}]"

        // Now that we have a port, initialize log writer
        java.io.File("logs").mkdirs()
        logWriter = PrintWriter(BufferedWriter(
            FileWriter("logs/Bidder_${bidderInstanceId}_journal.log", true)
        ))
        metrics = MetricsCollector("BidderMicroservice_$bidderInstanceId")

        if (checkAndRecoverFromJournal(bidderInstanceId)) {
            val recovered = loadRecoveredBid()
            recoveredBidBody = recovered?.body
            logEvent("RECOVERY: Ciclu anterior intrerupt. Oferta recuperata: ${recoveredBidBody ?: "nu exista state local"}.")
        }

        logEvent("CYCLE_START: Connected to Auctioneer. Identity: name='$bidderName', phone='$bidderPhone', email='$bidderEmail'.")
        metrics.record("cycle_start", mapOf(
            "bidder_id" to bidderInstanceId,
            "name" to bidderName,
            "phone" to bidderPhone,
            "email" to bidderEmail
        ))
        println("$myIdentity M-am conectat la Auctioneer! Identitate: $bidderName ($bidderPhone, $bidderEmail)")

        // HeartBeat (Homework 2)
        heartBeatClient = HeartBeatClient("BidderMicroservice:$bidderInstanceId")
        heartBeatClient.start()

        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            metrics.record("shutdown", mapOf("bidder_id" to bidderInstanceId))
            metrics.stop()
            heartBeatClient.stop()
            runCatching { logWriter.close() }
        })

        // Build Observable for auction result
        auctionResultObservable = Observable.create<String> { emitter ->
            val bufferReader = BufferedReader(InputStreamReader(auctioneerSocket.inputStream))
            val receivedMessage = bufferReader.readLine()

            if (receivedMessage == null) {
                bufferReader.close()
                auctioneerSocket.close()
                emitter.onError(Exception("AuctioneerMicroservice s-a deconectat."))
                return@create
            }

            emitter.onNext(receivedMessage)
            emitter.onComplete()

            bufferReader.close()
            auctioneerSocket.close()
        }
    }

    // ── Bidding logic ─────────────────────────────────────────────────────────

    private fun bid() {
        val recoveredPrice = recoveredBidBody
            ?.substringAfter("licitez ", missingDelimiterValue = "")
            ?.toIntOrNull()
        val pret = recoveredPrice ?: Random.nextInt(MIN_BID, MAX_BID)

        // Oferta contine si identitatea ceruta in tema de acasa, nu doar pretul.
        val biddingMessage = Message.createWithIdentity(
            sender = "${auctioneerSocket.localAddress}:${auctioneerSocket.localPort}",
            body   = "licitez $pret",
            name   = bidderName,
            phone  = bidderPhone,
            email  = bidderEmail
        )

        logEvent("Placing bid: licitez $pret")
        metrics.record("bid_sent", mapOf("bidder_id" to bidderInstanceId, "price" to pret.toString()))
        persistBidForRecovery(biddingMessage)
        val serializedMessage = biddingMessage.serialize()
        val output = auctioneerSocket.getOutputStream()
        output.write(serializedMessage)

        // 50% chance duplicate — exercises MessageProcessor's deduplication (Lab Task 2)
        if (Random.nextBoolean()) {
            logEvent("Sending duplicate (deduplication test).")
            metrics.record("duplicate_sent", mapOf("bidder_id" to bidderInstanceId, "price" to pret.toString()))
            output.write(serializedMessage)
        }

        output.flush()
        // Inchidem doar directia de trimitere. Socketul ramane deschis pentru rezultat,
        // iar Auctioneer poate citi corect toate liniile trimise de acest Bidder.
        auctioneerSocket.shutdownOutput()
    }

    private fun waitForResult() {
        println("$myIdentity Astept rezultatul licitatiei...")
        logEvent("Waiting for auction result.")

        val sub = auctionResultObservable.subscribeBy(
            onNext = { raw ->
                val resultMessage = Message.deserialize(raw.toByteArray())
                println("$myIdentity Rezultat licitatie: ${resultMessage.body}")
                logEvent("CYCLE_END: Auction result: ${resultMessage.body}")
                metrics.record("auction_result", mapOf(
                    "bidder_id" to bidderInstanceId,
                    "result" to resultMessage.body
                ))
                clearRecoveredBid()
                metrics.stop()
                heartBeatClient.stop()
                logWriter.close()
            },
            onError = { err ->
                println("$myIdentity Eroare: $err")
                logEvent("ERROR: $err")
                metrics.record("error", mapOf("bidder_id" to bidderInstanceId, "message" to err.message.orEmpty()))
                metrics.stop()
                heartBeatClient.stop()
                logWriter.close()
            }
        )

        sub.dispose()
    }

    fun run() {
        bid()
        waitForResult()
    }
}

fun main(args: Array<String>) {
    val bidderIndex = args.firstOrNull()?.toIntOrNull()
    BidderMicroservice(bidderIndex).run()
}
