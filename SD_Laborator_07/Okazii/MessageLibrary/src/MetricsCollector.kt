import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * MetricsCollector (tema 3 - instrumentare)
 *
 * Clasa asta este folosita de toate microserviciile pentru doua lucruri:
 * 1. scrie metricile local, in logs/<ServiceName>_metrics.log;
 * 2. scrie aceleasi evenimente si in logs/master_metrics.log.
 * Pentru master log folosim FileLock, deoarece fiecare microserviciu ruleaza in
 * alt proces JVM si un simplu synchronized nu sincronizeaza procese diferite.
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
    private val flushScheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "MetricsFlush-$serviceName").apply { isDaemon = true }
    }

    companion object {
        val DATE_FMT = SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS")

        /**
         * Scriere atomica pentru logul comun. @Synchronized ajuta in acelasi JVM,
         * iar FileLock ajuta intre procese JVM diferite.
         */
        @Synchronized
        fun appendToMasterLog(line: String) {
            try {
                appendLinesWithLock("logs/master_metrics.log", listOf(line))
            } catch (e: Exception) {
                System.err.println("[MetricsCollector] ERROR writing to master log: $e")
            }
        }

        private fun appendLinesWithLock(path: String, lines: List<String>) {
            if (lines.isEmpty()) return

            val file = File(path)
            file.parentFile?.mkdirs()

            RandomAccessFile(file, "rw").use { raf ->
                val channel = raf.channel
                channel.lock().use {
                    channel.position(channel.size())
                    val payload = lines.joinToString(
                        separator = System.lineSeparator(),
                        postfix = System.lineSeparator()
                    )
                    channel.write(StandardCharsets.UTF_8.encode(payload))
                }
            }
        }
    }

    init {
        java.io.File("logs").mkdirs()

        // La 2 secunde golim bufferul in fisiere ca sa nu scriem pe disc la fiecare apel record().
        flushScheduler.scheduleAtFixedRate({ flush() }, 2, 2, TimeUnit.SECONDS)
    }

    /**
     * Adauga o metrica in buffer. Scrierea pe disc se face in flush().
     */
    fun record(event: String, tags: Map<String, String> = emptyMap()) {
        val metric = MetricEvent(System.currentTimeMillis(), serviceName, event, tags)
        buffer.add(metric)
        totalEventsRecorded.incrementAndGet()
    }

    /**
     * Scrie toate evenimentele atat in logul local, cat si in master log.
     */
    @Synchronized
    fun flush() {
        if (buffer.isEmpty()) return

        val events = mutableListOf<MetricEvent>()
        while (buffer.isNotEmpty()) {
            buffer.poll()?.let { events.add(it) }
        }

        try {
            appendLinesWithLock(localLogPath, events.map { it.toString() })
        } catch (e: Exception) {
            System.err.println("[MetricsCollector] ERROR writing to local log: $e")
        }

        // Master log-ul este comun pentru toate procesele.
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
