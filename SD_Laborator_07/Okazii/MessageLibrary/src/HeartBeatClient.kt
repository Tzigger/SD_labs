import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * HeartBeatClient
 *
 * A utility class that each microservice instantiates to periodically send heartbeat
 * messages to HeartBeatMicroservice, proving that it is alive and healthy.
 *
 * Usage (in any microservice init block):
 *   private val heartBeatClient = HeartBeatClient("AuctioneerMicroservice")
 *   // ... in run() or after init:
 *   heartBeatClient.start()
 *   // ... on shutdown:
 *   heartBeatClient.stop()
 *
 * The heartbeat message format sent to HeartBeatMicroservice is:
 *   "heartbeat <SERVICE_NAME>\n"
 */
class HeartBeatClient(
    private val serviceName: String,
    private val heartBeatHost: String = "localhost",
    private val heartBeatPort: Int = 2000,
    private val intervalMs: Long = 3_000L
) {
    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "HeartBeat-$serviceName").apply { isDaemon = true }
    }
    private var writer: PrintWriter? = null
    private var socket: Socket? = null

    fun start() {
        try {
            socket = Socket(heartBeatHost, heartBeatPort)
            writer = PrintWriter(socket!!.getOutputStream(), true)
            println("[$serviceName] HeartBeat client connected to HeartBeatMicroservice.")
        } catch (e: Exception) {
            println("[$serviceName] WARNING: Cannot connect to HeartBeatMicroservice: $e. Running without heartbeat monitoring.")
            return
        }

        scheduler.scheduleAtFixedRate({
            try {
                writer?.println("heartbeat $serviceName")
            } catch (e: Exception) {
                println("[$serviceName] WARNING: HeartBeat send failed: $e")
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS)
    }

    fun stop() {
        scheduler.shutdownNow()
        runCatching { writer?.close() }
        runCatching { socket?.close() }
    }
}
