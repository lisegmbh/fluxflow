package de.lise.fluxflow.stereotyped.job

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import de.lise.fluxflow.stereotyped.continuation.ContinuationBuilder
import de.lise.fluxflow.stereotyped.continuation.ContinuationConverter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant

class JobDefinitionBuilderTest {

    private val parameterResolver = mock<ParameterResolver> {}

    private val jobDefinitionBuilder = JobDefinitionBuilder(
        mock {},
        mock {},
        parameterResolver,
        mock {},
        mutableMapOf()
    )

    @Test
    fun `build should not throw an exception when there is only one public function`() {
        // Arrange
        val testJob = TestJobWithConventionBasedPayloadFunction()

        // Act & Assert
        assertDoesNotThrow {
            jobDefinitionBuilder.build(testJob)
        }
    }

    @Test
    fun `build should throw an exception if there are multiple possible payload functions`() {
        // Arrange
        val testJob = TestJobWithAmbiguousPayloadFunctions()

        // Act & Assert
        val exception = assertThrows<JobConfigurationException> {
            jobDefinitionBuilder.build(testJob)
        }
        assertThat(exception).hasMessageStartingWith("Found multiple")
    }

    @Test
    fun `build should throw an exception if there is no suitable payload function`() {
        // Arrange
        val testJob = TestJobWithoutPayloadFunction()

        // Act & Assert
        val exception = assertThrows<JobConfigurationException> {
            jobDefinitionBuilder.build(testJob)
        }
        assertThat(exception).hasMessageStartingWith("Could not find")
    }

    @Test
    fun `build should not throw an exception if there are multiple payload functions but one of them is annotated with @JobPayload`() {
        // Arrange
        val testJob = TestJobWithExplicitPayloadFunction()

        // Act & Assert
        assertDoesNotThrow {
            jobDefinitionBuilder.build(testJob)
        }
    }

    @Test
    fun `build should throw an exception if multiple functions are annotated with @JobPayload`() {
        // Arrange
        val testJob = TestJobWithAmbiguousExplicitPayloadFunctions()

        // Act & Assert
        val exception = assertThrows<JobConfigurationException> {
            jobDefinitionBuilder.build(testJob)
        }
        assertThat(exception)
            .hasMessageStartingWith("Found multiple explicitly")
    }

    @Test
    fun `build should get a continuation converter using the provided ContinuationBuilder`() {
        // Arrange
        val expectedContinuation = mock<Continuation<Any>> {}
        val continuationConverter = mock<ContinuationConverter<Any>> {
            on { toContinuation(any()) }.doReturn(expectedContinuation)
        }
        val continuationBuilder = mock<ContinuationBuilder> {
            on { createResultConverter<Any>(any(), any(), any()) }.doReturn(continuationConverter)
        }
        val jobDefinitionBuilder = JobDefinitionBuilder(
            mock {},
            continuationBuilder,
            parameterResolver,
            mock {},
            mutableMapOf()
        )
        val testJob = TestJob()

        // Act
        val continuation = jobDefinitionBuilder.build(testJob)
            .createJob(
                JobIdentifier("id"),
                mock<Workflow<Any>> {},
                Instant.now(),
                null,
                JobStatus.Scheduled
            ).execute()

        // Assert
        verify(continuationBuilder, times(1)).createResultConverter(
            any(),
            any(),
            eq(TestJob::payloadFunction)
        )
        assertThat(continuation).isSameAs(expectedContinuation)
    }

    @Test
    fun `build should return a job definition that calls the actual payload function`() {
        // Arrange
        val continuationConverter = mock<ContinuationConverter<Any>> {}
        val continuationBuilder = mock<ContinuationBuilder> {
            on {
                createResultConverter<Any>(
                    any(),
                    any(),
                    any()
                )
            }.doReturn(continuationConverter)
        }
        val jobDefinitionBuilder = JobDefinitionBuilder(
            mock {},
            continuationBuilder,
            parameterResolver,
            mock {},
            mutableMapOf()
        )
        val testJob = mock<TestJob> {}

        // Act
        jobDefinitionBuilder.build(testJob)
            .createJob(
                JobIdentifier("id"),
                mock<Workflow<Any>> {},
                Instant.now(),
                null,
                JobStatus.Scheduled
            ).execute()

        // Assert
        verify(testJob, times(1)).payloadFunction()
    }

    private class TestJobWithConventionBasedPayloadFunction {
        private fun `i am a private function an can not be used as the payload function`() {}
        fun `i am the only public function and will therefore be used as the payload function`() {}
    }

    private class TestJobWithAmbiguousPayloadFunctions {
        fun `do something`() {}
        fun `do something else`() {}
    }

    private class TestJobWithoutPayloadFunction {
        private fun `i can not be used because i am private`() {}

        companion object {
            @JvmStatic
            fun `i can not be used because i am static`() {
            }
        }
    }

    private class TestJobWithExplicitPayloadFunction {
        fun `i do something other than executing the job`() {}

        @JobPayload
        fun `i am the function you are looking for`() {
        }
    }

    private class TestJobWithAmbiguousExplicitPayloadFunctions {
        @JobPayload
        fun `i am en explicitly annotated payload function`() {
        }

        @JobPayload
        fun `so am i`() {
        }
    }

    private class TestJobWithPayloadFunctionRequiringParameters {
        @Suppress("UNUSED_PARAMETER")
        fun `i am the payload function`(someParam: String) {
        }
    }

    class TestJob {
        fun payloadFunction() {}
    }
}