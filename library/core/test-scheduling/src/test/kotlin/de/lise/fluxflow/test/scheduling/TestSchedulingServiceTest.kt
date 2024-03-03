package de.lise.fluxflow.test.scheduling

import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.scheduling.SchedulingReference
import de.lise.fluxflow.test.scheduling.util.BusyWait
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.byLessThan
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class TestSchedulingServiceTest {
    @Test
    fun `callbacks should be called after specified time`() {
        // Arrange
        val executedTime = Updatable<Instant>()
        val reference = SchedulingReference(
            WorkflowIdentifier("a-workflow"),
            JobIdentifier("a-job"),
            null,
            null,
        )
        val expectedTime = Instant.now().plusSeconds(3L)
        val testSchedulingService = TestSchedulingService(
            {
                executedTime.value = Instant.now()
            }
        )

        // Act
        testSchedulingService.schedule(expectedTime, reference)

        // Assert
        val actualExecutionTime = executedTime.busyWait(5)
        assertThat(actualExecutionTime).isCloseTo(
            expectedTime,
            byLessThan(100, ChronoUnit.MILLIS)
        )
    }

    @Test
    fun `callbacks should receive the provided scheduling ref`() {
        // Arrange
        val actualReferenceUpdatable = Updatable<SchedulingReference>()
        val expectedReference = SchedulingReference(
            WorkflowIdentifier("a-workflow"),
            JobIdentifier("a-job"),
            null,
            null,
        )
        val expectedTime = Instant.now().plusSeconds(1L)
        val testSchedulingService = TestSchedulingService(
            {
                actualReferenceUpdatable.value = it
            }
        )

        // Act
        testSchedulingService.schedule(expectedTime, expectedReference)

        // Assert
        val actualReference = actualReferenceUpdatable.busyWait(2)
        assertThat(actualReference).isEqualTo(expectedReference)
    }

    @Test
    fun `callbacks should be invoked immediately if the given time is in the past`() {
        // Arrange
        val updatableThread = Updatable<Thread>()
        val testSchedulingService = TestSchedulingService(
            {
                updatableThread.value = Thread.currentThread()
            }
        )

        // Act
        testSchedulingService.schedule(
            Instant.now().minusMillis(100L),
            SchedulingReference(
                WorkflowIdentifier("a-workflow"),
                JobIdentifier("a-job"),
                null,
                null,
            )
        )

        // Assert
        assertThat(updatableThread.value).isSameAs(Thread.currentThread())
    }

    @Test
    fun `multiple scheduled events should not block each other`() {
        // Arrange
        val fiveSecondUpdate = Updatable<Instant>()
        val threeSecondUpdate = Updatable<Instant>()
        val fiveSecondRef = SchedulingReference(WorkflowIdentifier("a-workflow"), JobIdentifier("5"), null, null)
        val threeSecondRef = SchedulingReference(WorkflowIdentifier("a-workflow"), JobIdentifier("3"), null, null)
        val testSchedulingService = TestSchedulingService(
            { ref ->
                when (ref) {
                    threeSecondRef -> threeSecondUpdate
                    fiveSecondRef -> fiveSecondUpdate
                    else -> null
                }?.let { it.value = Instant.now() }
            }
        )
        val now = Instant.now()
        val fiveSecondsWait = now.plusSeconds(5)
        val threeSecondsWait = now.plusSeconds(3)


        // Act
        // it is important to schedule the long-running one first, in order to prove that it is not blocking
        // the execution of the shorter one
        testSchedulingService.schedule(fiveSecondsWait, fiveSecondRef)
        testSchedulingService.schedule(threeSecondsWait, threeSecondRef)

        // Assert
        fiveSecondUpdate.busyWait(6)
        assertThat(fiveSecondUpdate.value).isCloseTo(
            fiveSecondsWait,
            byLessThan(500, ChronoUnit.MILLIS)
        )
        assertThat(threeSecondUpdate.value).isCloseTo(
            threeSecondsWait,
            byLessThan(500, ChronoUnit.MILLIS)
        )
    }


    data class Updatable<T>(
        var value: T? = null
    ) {
        fun busyWait(limitInSeconds: Int): T {
            return BusyWait.toBeNonNull(limitInSeconds) {
                value
            }
        }
    }
}