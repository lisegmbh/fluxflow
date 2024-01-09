package de.lise.fluxflow.stereotyped.job

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

class ReflectedJobTest {
    @Test
    fun `execute should invoke the provided method caller`() {
        // Arrange
        val jobCaller = mock<JobCaller<Any>> {}
        val jobModelInstance = Any()
        val reflectedJob = ReflectedJob(
            JobIdentifier("id"),
            mock {},
            Instant.now(),
            null,
            JobStatus.Scheduled,
            mock {},
            emptyList(),
            jobModelInstance,
            jobCaller
        )

        // Act
        reflectedJob.execute()

        // Assert
        verify(jobCaller, times(1)).call(reflectedJob, jobModelInstance)
    }

    @Test
    fun `execute should return the continuation returned by the method caller`() {
        // Arrange
        val expectedContinuation = mock<Continuation<*>> {}
        val jobCaller = mock<JobCaller<Any>> {
            on { call(any(), any()) }.doReturn(expectedContinuation)
        }
        val reflectedJob = ReflectedJob(
            JobIdentifier("id"),
            mock {},
            Instant.now(),
            null,
            JobStatus.Scheduled,
            mock {},
            emptyList(),
            Any(),
            jobCaller
        )

        // Act
        val returnedContinuation = reflectedJob.execute()

        // Assert
        assertThat(returnedContinuation).isSameAs(expectedContinuation)
    }
}