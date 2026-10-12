package de.lise.fluxflow.springboot.configuration

import de.fluxflow.flowquery.query.FlowQueryBuilder
import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobService
import de.lise.fluxflow.api.job.query.JobQueryable
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.persistence.job.ScheduledJobReferencePersistence
import de.lise.fluxflow.query.pagination.Page
import de.lise.fluxflow.scheduling.SchedulingReference
import de.lise.fluxflow.scheduling.SchedulingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import java.time.Instant

class BasicConfigurationReconciliationTest {
    @Test
    fun `factory without reference persistence schedules materialized jobs after one bulk read`() {
        val workflow = mock<Workflow<Any>> {
            on { identifier } doReturn WorkflowIdentifier("bulk-workflow")
        }
        val time = Instant.parse("2026-09-10T09:00:00Z")
        val items = listOf("first", "second").map { id ->
            mock<Job> {
                on { identifier } doReturn JobIdentifier(id)
                on { this.workflow } doReturn workflow
                on { scheduledTime } doReturn time
                on { cancellationKey } doReturn null
            }
        }
        val jobs = mock<JobService>()
        whenever(jobs.findAll(any<FlowQueryBuilder<JobQueryable, JobQueryable>>()))
            .thenReturn(Page.unpaged(items))
        val workflows = mock<WorkflowService>()
        val scheduler = mock<SchedulingService>()
        whenever(scheduler.isJobScheduled(any())).thenReturn(false)
        val provider = DefaultListableBeanFactory().getBeanProvider(ScheduledJobReferencePersistence::class.java)

        BasicConfiguration().startupJobReconciliation(jobs, scheduler, provider, workflows).setup()

        val scheduled = argumentCaptor<SchedulingReference>()
        verify(scheduler, org.mockito.kotlin.times(2)).schedule(any(), scheduled.capture())
        assertThat(scheduled.allValues.map { it.jobIdentifier.value }).containsExactly("first", "second")
        verify(jobs).findAll(any<FlowQueryBuilder<JobQueryable, JobQueryable>>())
        verify(jobs, never()).getJob(any<Workflow<Any>>(), any())
        verifyNoInteractions(workflows)
    }
}
