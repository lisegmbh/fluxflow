package de.lise.fluxflow.stereotyped.job

import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobKind
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.workflow.Workflow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.Instant

class ReflectedJobDefinitionTest {

    @Test
    fun `createJob should return a job with the provided information`() {
        // Arrange
        val instance = Any()
        val jobCaller = mock<JobCaller<Any>> { }
        val reflectedJobDefinition = ReflectedJobDefinition(
            JobKind("kind"),
            emptyList(),
            emptyMap(),
            instance,
            jobCaller
        )

        val jobIdentifier = JobIdentifier("id")
        val workflow = mock<Workflow<*>> {}
        val instant = Instant.now()

        // Act
        val job = reflectedJobDefinition.createJob(
            jobIdentifier,
            workflow,
            instant,
            null,
            JobStatus.Scheduled
        )

        // Assert
        assertThat(job.identifier).isSameAs(jobIdentifier)
        assertThat(job.definition).isSameAs(reflectedJobDefinition)
        assertThat(job.workflow).isSameAs(workflow)
        assertThat(job.scheduledTime).isSameAs(instant)
    }
}