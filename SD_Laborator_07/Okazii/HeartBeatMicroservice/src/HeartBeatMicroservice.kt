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
import java.util.concurrent.TimeUnit

/**
 * HeartBeatMicroservice (Homework Task 2)
 *
 * Health monitor for all Okazii microservices.
 * - Each service connects to port 2000 on startup via HeartBeatClient and sends
 *   periodic "heartbeat <SERVICE_NAME>" messages.
 * - This service tracks the last heartbeat time for each registered service.
 * - If a service exceeds HEARTBEAT_TIMEOUT_MS without a heartbeat, it is declared
 *   failed and `docker restart <container-name>` is invoked.
 *
 * Shutdown: responds correctly to CTRL+C via a JVM shutdown hook.
 */
class HeartBeatMicroservice {

    // Registry: serviceName → last heartbeat timestamp (ms)
    private val heartbeatRegistry: MutableMap<String, Long> = mutableMapOf()
    private val subscriptions = CompositeDisposable()
    private val heartbeatServerSocket: ServerSocket
    private val logWriter: PrintWriter

    companion object Constants {
        const val HEARTBEAT_PORT        = 2000
        const val HEARTBEAT_TIMEOUT_MS  = 10_000L  // dead if silent for this long
        const val MONITOR_INTERVAL_MS   = 5_000L   // check interval
        val DATE_FMT = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")

        /** Maps service display name → Docker container name */
        val CONTAINER_NAMES = mapOf(
            "AuctioneerMicroservice"        to "okazii_auctioneer",
            "MessageProcessorMicroservice"  to "okazii_msgprocessor",
            "BiddingProcessorMicroservice"  to "okazii_biddingprocessor"
        )
    }

    // ── Logging ───────────────────────────────────────────────────────────────

    private fun logEvent(event: String) {
        val ts = DATE_FMT.format(Date())
        val line = "[$ts] [HeartBeat] $event"
        println(line)
        logWriter.println(line)
        logWriter.flush()
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        java.io.File("logs").mkdirs()
        logWriter = PrintWriter(BufferedWriter(FileWriter("logs/HeartBeat_journal.log", true)))

        heartbeatServerSocket = ServerSocket(HEARTBEAT_PORT)
        logEvent("HeartBeatMicroservice started on port $HEARTBEAT_PORT.")
        println("HeartBeatMicroservice se executa pe portul: $HEARTBEAT_PORT")
        println("Apasati CTRL+C pentru a opri.")

        // Graceful CTRL+C shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            logEvent("Shutdown signal received. Stopping HeartBeatMicroservice...")
            subscriptions.dispose()
            runCatching { heartbeatServerSocket.close() }
            logEvent("HeartBeatMicroservice stopped.")
            logWriter.close()
        })
    }

    // ── Heartbeat Reception ───────────────────────────────────────────────────

    private fun startAcceptingHeartbeats() {
        val acceptObservable = Observable.create<Socket> { emitter ->
            while (!heartbeatServerSocket.isClosed) {
                try {
                    val conn = heartbeatServerSocket.accept()
                    emitter.onNext(conn)
                } catch (e: Exception) {
                    if (!heartbeatServerSocket.isClosed) logEvent("Accept error: $e")
                    break
                }
            }
            emitter.onComplete()
        }

        subscriptions.add(
            acceptObservable.subscribeBy(
                onNext = { socket ->
                    Thread { handleServiceConnection(socket) }.apply {
                        isDaemon = true
                        name = "HB-handler-${socket.remoteSocketAddress}"
                        start()
                    }
                },
                onComplete = { logEvent("Accept loop completed.") },
                onError    = { err -> logEvent("Accept error: $err") }
            )
        )
    }

    private fun handleServiceConnection(socket: Socket) {
        val reader = BufferedReader(InputStreamReader(socket.inputStream))
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.trim().split(" ", limit = 2)
                if (parts.size == 2 && parts[0] == "heartbeat") {
                    val serviceName = parts[1]
                    val now = System.currentTimeMillis()
                    heartbeatRegistry[serviceName] = now
                    logEvent("Heartbeat from '$serviceName'.")
                }
            }
        } catch (e: Exception) {
            logEvent("Connection to ${socket.remoteSocketAddress} closed: $e")
        } finally {
            runCatching { reader.close() }
            runCatching { socket.close() }
        }
    }

    // ── Health Monitoring ─────────────────────────────────────────────────────

    private fun startMonitoring() {
        subscriptions.add(
            Observable.interval(MONITOR_INTERVAL_MS, TimeUnit.MILLISECONDS).subscribeBy(
                onNext = { tick ->
                    val now = System.currentTimeMillis()
                    logEvent("Monitor tick #$tick — ${heartbeatRegistry.size} registered services.")
                    heartbeatRegistry.forEach { (name, lastHb) ->
                        val age = now - lastHb
                        if (age > HEARTBEAT_TIMEOUT_MS) {
                            logEvent("FAILURE: '$name' silent for ${age}ms. Restarting...")
                            restartService(name)
                        } else {
                            logEvent("OK: '$name' — last HB ${age}ms ago.")
                        }
                    }
                },
                onError = { err -> logEvent("Monitor error: $err") }
            )
        )
    }

    private fun restartService(serviceName: String) {
        val containerName = when {
            serviceName.startsWith("BidderMicroservice:") ->
                "okazii_bidder_${serviceName.substringAfter(":")}"
            else -> CONTAINER_NAMES[serviceName]
        }

        if (containerName == null) {
            logEvent("WARNING: No container mapping for '$serviceName'.")
            return
        }

        logEvent("Running: docker restart $containerName")
        try {
            val proc = ProcessBuilder("docker", "restart", containerName)
                .redirectErrorStream(true).start()
            val output = proc.inputStream.bufferedReader().readText().trim()
            val code = proc.waitFor()
            if (code == 0) {
                logEvent("Restarted '$containerName' successfully.")
                heartbeatRegistry.remove(serviceName)
            } else {
                logEvent("docker restart failed (exit $code): $output")
            }
        } catch (e: Exception) {
            logEvent("ERROR executing docker restart: $e")
        }
    }

    // ── Entry Point ───────────────────────────────────────────────────────────

    fun run() {
        startAcceptingHeartbeats()
        startMonitoring()
        // Block main thread — shutdown hook handles CTRL+C
        Thread.currentThread().join()
    }
}

fun main(args: Array<String>) {
    HeartBeatMicroservice().run()
}
