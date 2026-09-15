package de.lise.fluxflow.springboot.bootstrapping

import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobNotFoundException
import de.lise.fluxflow.api.job.JobService
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.persistence.job.ScheduledJobReference
import de.lise.fluxflow.persistence.job.ScheduledJobReferencePersistence
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.UnknownTypeException
import de.lise.fluxflow.scheduling.SchedulingReference
import de.lise.fluxflow.scheduling.SchedulingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

class ReconcileScheduledJobsBootstrapActionTest {
    @Test
    fun `O01 isolates rejected and missing jobs between healthy scheduled jobs`() {
        val references = listOf(
            reference("workflow-1", "good-before"),
            reference("workflow-1", "rejected"),
            reference("workflow-1", "missing-job"),
            reference("missing-workflow", "missing"),
            reference("workflow-1", "good-after"),
        )
        val referencePersistence = mock<ScheduledJobReferencePersistence>()
        whenever(referencePersistence.findScheduledJobReferences()).thenReturn(references)
        val workflow = mock<Workflow<Any>>()
        val workflowService = mock<WorkflowService>()
        whenever(workflowService.get<Any>(WorkflowIdentifier("workflow-1"))).thenReturn(workflow)
        doThrow(IllegalStateException("workflow missing"))
            .whenever(workflowService).get<Any>(WorkflowIdentifier("missing-workflow"))
        val jobService = mock<JobService>()
        val goodBefore = job("good-before", workflow, Instant.parse("2026-09-10T09:00:00Z"))
        val goodAfter = job("good-after", workflow, Instant.parse("2026-09-10T10:00:00Z"))
        whenever(jobService.getJob(workflow, JobIdentifier("good-before")))
            .thenReturn(goodBefore)
        whenever(jobService.getJob(workflow, JobIdentifier("good-after")))
            .thenReturn(goodAfter)
        doThrow(UnknownTypeException(TypeRole.VALUE, "untrusted.Value"))
            .whenever(jobService).getJob(workflow, JobIdentifier("rejected"))
        doThrow(JobNotFoundException(workflow, JobIdentifier("missing-job")))
            .whenever(jobService).getJob(workflow, JobIdentifier("missing-job"))
        val schedulingService = mock<SchedulingService>()
        whenever(schedulingService.isJobScheduled(any())).thenReturn(false)

        ReconcileScheduledJobsBootstrapAction(
            jobService,
            schedulingService,
            referencePersistence,
            workflowService,
        ).setup()

        val scheduled = argumentCaptor<SchedulingReference>()
        verify(schedulingService, org.mockito.kotlin.times(2)).schedule(any(), scheduled.capture())
        assertThat(scheduled.allValues.map { it.jobIdentifier.value })
            .containsExactly("good-before", "good-after")
    }

    private fun reference(workflowId: String, jobId: String) = ScheduledJobReference(
        WorkflowIdentifier(workflowId),
        JobIdentifier(jobId),
    )

    private fun job(identifier: String, workflow: Workflow<*>, scheduledTime: Instant): Job =
        mock {
            on { this.identifier } doReturn JobIdentifier(identifier)
            on { this.workflow } doReturn workflow
            on { this.scheduledTime } doReturn scheduledTime
            on { cancellationKey } doReturn null
        }
}
