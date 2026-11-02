package dev.nytheria.compat.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import dev.nytheria.compat.loader.NytheriaCompatFlags

/**
 * Nytheria Compat: wall-clock timings of the translator's startup phases. Logged at DEBUG, or at INFO with `-Dnytheriacompat.printTimings=true`.
 */
object NytheriaCompatTimings {
    @JvmField
    val logger: Logger = LoggerFactory.getLogger("Nytheria Compat Timings")

    @JvmStatic
    fun log(phase: String, nanos: Long) {
        if (!NytheriaCompatFlags.PRINT_TIMINGS && !logger.isDebugEnabled)
            return

        val ms = "%.1f".format(nanos / 1_000_000.0)
        if (NytheriaCompatFlags.PRINT_TIMINGS)
            logger.info("[timing] {} took {} ms", phase, ms)
        else
            logger.debug("[timing] {} took {} ms", phase, ms)
    }

    inline fun <T> time(phase: String, block: () -> T): T {
        val start = System.nanoTime()
        try {
            return block()
        } finally {
            log(phase, System.nanoTime() - start)
        }
    }
}
