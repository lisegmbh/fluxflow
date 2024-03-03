package de.lise.fluxflow.test.scheduling.util

import java.time.Duration
import java.time.Instant
import java.time.temporal.TemporalAmount

data class BusyWait<T>(
    private val provider: () -> T,
    private val condition: (element: T) -> Boolean,
    private val backoffMillis: Long = 100
) {
    fun waitFor(duration: TemporalAmount): T {
        val endOfWait = Instant.now().plus(duration)
        var currentValue: T?
        do {
            currentValue = provider()
            if (condition(currentValue)) {
                return currentValue
            }
            Thread.sleep(backoffMillis)
            currentValue = provider()
            if (condition(currentValue)) {
                return currentValue
            }
        } while (endOfWait.isAfter(Instant.now()))
        throw InterruptedException("Wait limit reached")
    }

    fun waitForSeconds(duration: Int): T {
        return waitFor(Duration.ofSeconds(duration.toLong()))
    }

    companion object {

        @JvmStatic
        fun <T> toBeNonNull(
            timeoutInSeconds: Int,
            provider: () -> T?
        ): T {
            return BusyWait(
                provider,
                { it != null}
            ).waitForSeconds(timeoutInSeconds)!!
        }

        @JvmStatic
        fun toBeTrue(
            timeoutInSeconds: Int,
            provider: () -> Boolean
        ) {
            BusyWait(
                provider,
                { it }
            ).waitForSeconds(timeoutInSeconds)
        }
    }
}