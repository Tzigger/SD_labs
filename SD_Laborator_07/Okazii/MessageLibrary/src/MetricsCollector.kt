import java.io.BufferedWriter
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * MetricsCollector  (Homework Task 3 — Instrumentation)
 *
 * A thread-safe, singleton-style metrics collection utility shared across all microservices.
 *
 * Responsibilities:
 *   1. Each microservice writes metrics locally via its own MetricsCollector instance
 *      (backed by a local file: logs/<ServiceName>_metrics.log).
 *   2. Additionally, every metric event is also appended to a shared master metrics log
 *      at logs/master_metrics.log — this is the "generalized log synchronized with local
 *      journals" required by the homework.
 *   3. A background scheduler periodically flushes in-memory metrics to disk.
 *
 * Thread-safety: All writes are serialized through a single-threaded scheduler / synchronized blocks.
 *
 * Usage:
 *   val metrics = MetricsCollector("AuctioneerMicroservice")
 *   metrics.record("bid_received", mapOf("sender" to "localhost:50023", "price" to "5200"))
 *   metrics.record("auction_closed", mapOf("duration_ms" to "15003", "total_bids" to "47"))
 *   metrics.stop()  // flush and close on shutdown
 */
class MetricsCollector(private val serviceName: String) {

    data class MetricEvent(
        val timestamp: Long,
        val service: String,
        val event: String,
        val tags: Map<String, String>
    ) {
        override fun toString(): String {
            val ts = DATE_FMT.format(Date(timestamp))
            val tagStr = tags.entries.joinToString(", ") { "${it.key}=${it.value}" }
            return "[$ts] [$service] EVENT=$event ${if (tagStr.isNotEmpty()) "| $tagStr" else ""}"
        }
    }

    private val buffer: ConcurrentLinkedQueue<MetricEvent> = ConcurrentLinkedQueue()
    private val totalEventsRecorded = AtomicLong(0)

    private val localLogPath  = "logs/${serviceName}_metrics.log"
    private val masterLogPath = "logs/master_metrics.log"

    private val flushScheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "MetricsFlush-$serviceName").apply { isDaemon = true }
    }

    companion object {
        val DATE_FMT = SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS")

        /**
         * Write a line atomically to a shared file. Synchronized on the file path string's
         * intern to serialize cross-service writes to the master log.
         */
        @Synchronized
        fun appendToMasterLog(line: String) {
            try {
                PrintWriter(BufferedWriter(FileWriter("logs/master_metrics.log", true))).use {
                    it.println(line)
                }
            } catch (e: Exception) {
                System.err.println("[MetricsCollector] ERROR writing to master log: $e")
            }
        }
    }

    init {
        java.io.File("logs").mkdirs()

        // Schedule periodic flush every 2 seconds
        flushScheduler.scheduleAtFixedRate({ flush() }, 2, 2, TimeUnit.SECONDS)
    }

    /**
     * Record a metric event. Thread-safe — can be called from any thread.
     */
    fun record(event: String, tags: Map<String, String> = emptyMap()) {
        val metric = MetricEvent(System.currentTimeMillis(), serviceName, event, tags)
        buffer.add(metric)
        totalEventsRecorded.incrementAndGet()
    }

    /**
     * Flush all buffered metric events to both the local log and the master log.
     */
    @Synchronized
    fun flush() {
        if (buffer.isEmpty()) return

        val events = mutableListOf<MetricEvent>()
        while (buffer.isNotEmpty()) {
            buffer.poll()?.let { events.add(it) }
        }

        try {
            // Write to local service log
            PrintWriter(BufferedWriter(FileWriter(localLogPath, true))).use { writer ->
                events.forEach { writer.println(it.toString()) }
            }
        } catch (e: Exception) {
            System.err.println("[MetricsCollector] ERROR writing to local log: $e")
        }

        // Write to shared master log (synchronized across JVM instances via file append)
        events.forEach { appendToMasterLog(it.toString()) }
    }

    /**
     * Flush remaining events and shut down the flush scheduler.
     */
    fun stop() {
        flush()
        flushScheduler.shutdown()
        flushScheduler.awaitTermination(5, TimeUnit.SECONDS)
    }

    fun totalEventsRecorded(): Long = totalEventsRecorded.get()
}
