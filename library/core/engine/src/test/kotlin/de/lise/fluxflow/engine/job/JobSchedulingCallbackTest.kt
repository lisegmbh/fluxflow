package de.lise.fluxflow.engine.job

import de.lise.fluxflow.api.interceptors.FlowInterceptor
import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.job.interceptors.JobExecutionContext
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowUpdateService
import de.lise.fluxflow.engine.continuation.ContinuationService
import de.lise.fluxflow.engine.interceptors.InterceptedInvocation
import de.lise.fluxflow.engine.workflow.WorkflowQueryServiceImpl
import de.lise.fluxflow.scheduling.SchedulingReference
import de.lise.fluxflow.stereotyped.continuation.ContinuationBuilder
import de.lise.fluxflow.stereotyped.job.JobDefinitionBuilder
import de.lise.fluxflow.stereotyped.job.parameter.ParameterDefinitionBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

class JobSchedulingCallbackTest {
    @Test
    fun `onScheduled should update the job status to running before execution and to executed before proceeding with the continuation`() {
        // Arrange
        val jobIdentifier = JobIdentifier("job")
        val testRef = SchedulingReference(
            WorkflowIdentifier("workflow"),
            jobIdentifier,
            null,
            null,
        )
        val testWorkflow = mock<Workflow<Any>> {}
        val workflowService = mock<WorkflowQueryServiceImpl> {
            on { get<Any>(any()) } doReturn testWorkflow
        }
        val workflowUpdateService = mock<WorkflowUpdateService> {}
        val testJob = mock<Job> {
            on { execute() } doReturn mock {}
            on { identifier } doReturn jobIdentifier
            on { workflow } doReturn testWorkflow
        }
        val jobService = mock<JobServiceImpl> {
            on {
                getJobOrNull<Any>(
                    any(),
                    any()
                )
            } doReturn testJob
            on {
                setStatus(
                    any(),
                    any()
                )
            } doReturn testJob
        }
        val continuationService = mock<ContinuationService> {}

        val schedulingCallback = JobSchedulingCallback(
            workflowService,
            workflowUpdateService,
            jobService,
            continuationService,
            InterceptedInvocation.empty()
        )

        // Act
        schedulingCallback.onScheduled(testRef)

        // Assert
        val executionOrder = inOrder(
            jobService,
            testJob,
            continuationService
        )
        executionOrder.verify(jobService).setStatus(
            any(),
            eq(JobStatus.Running)
        )
        executionOrder.verify(testJob).execute()
        executionOrder.verify(jobService).setStatus(
            any(),
            eq(JobStatus.Executed)
        )
        executionOrder.verify(continuationService).execute(
            any(),
            anyOrNull(),
            any(),
            any()
        )
    }

    @Test
    fun `onScheduled should update the job status to running before execution and to failed on thrown exception`() {
        // Arrange
        val testRef = SchedulingReference(
            WorkflowIdentifier("workflow"),
            JobIdentifier("job"),
            null,
            null,
        )
        val workflowService = mock<WorkflowQueryServiceImpl> {
            on { get<Any>(any()) }.doReturn(mock {})
        }
        val workflowUpdateService = mock<WorkflowUpdateService> {}
        val testJob = mock<Job> {
            on { execute() }.doThrow(RuntimeException("surprise surprise"))
        }
        val jobService = mock<JobServiceImpl> {
            on {
                getJobOrNull<Any>(
                    any(),
                    any()
                )
            }.doReturn(testJob)
            on {
                setStatus(
                    any(),
                    any()
                )
            }.doReturn(testJob)
        }
        val continuationService = mock<ContinuationService> {}

        val schedulingCallback = JobSchedulingCallback(
            workflowService,
            workflowUpdateService,
            jobService,
            continuationService,
            InterceptedInvocation.empty()
        )

        // Act
        schedulingCallback.onScheduled(testRef)

        // Assert
        val executionOrder = inOrder(
            jobService,
            testJob,
        )
        executionOrder.verify(jobService).setStatus(
            any(),
            eq(JobStatus.Running)
        )
        executionOrder.verify(testJob).execute()
        executionOrder.verify(jobService).setStatus(
            any(),
            eq(JobStatus.Failed)
        )
        verify(
            continuationService,
            never()
        ).execute(
            any(),
            any(),
            any(),
            any()
        )
        verify(
            jobService,
            never()
        ).setStatus(
            any(),
            eq(JobStatus.Executed)
        )
    }

    @Test
    fun `onScheduled should not execute the job and leave it status unchanged if an interceptor voted to abort the execution`() {
        // Arrange
        val testRef = SchedulingReference(
            WorkflowIdentifier("workflow"),
            JobIdentifier("job"),
            null,
            null,
        )
        val workflowService = mock<WorkflowQueryServiceImpl> {
            on { get<Any>(any()) }.doReturn(mock {})
        }
        val workflowUpdateService = mock<WorkflowUpdateService> {}
        val testJob = mock<Job> {
            on { execute() }.doThrow(RuntimeException("surprise surprise"))
        }
        val jobService = mock<JobServiceImpl> {
            on {
                getJobOrNull<Any>(
                    any(),
                    any()
                )
            }.doReturn(testJob)
            on {
                setStatus(
                    any(),
                    any()
                )
            }.doReturn(testJob)
        }
        val continuationService = mock<ContinuationService> {}

        var interceptorCalled = false
        val interceptedInvocation = InterceptedInvocation<JobExecutionContext>(
            listOf(
            FlowInterceptor { token ->
                token.abort()
                interceptorCalled = true
            }
        ))

        val schedulingCallback = JobSchedulingCallback(
            workflowService,
            workflowUpdateService,
            jobService,
            continuationService,
            interceptedInvocation,
        )

        // Act
        schedulingCallback.onScheduled(testRef)

        // Assert
        assertThat(interceptorCalled).isTrue
        verify(jobService, never()).setStatus(
            any(), 
            any()
        )
        verify(continuationService, never()).execute(
            any(),
            any(),
            any(),
            any()
        )
        verify(testJob, never()).execute()
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `onScheduled should persist workflow changes after job has been executed`() {
        // Arrange
        val jobIdentifier = JobIdentifier("job")
        val testRef = SchedulingReference(
            WorkflowIdentifier("workflow"),
            jobIdentifier,
            null,
            null,
        )
        val modifiableWorkflowData = ModifiableWorkflowData(false)
        val workflow = mock<Workflow<ModifiableWorkflowData>> {
            on { model }.doReturn(modifiableWorkflowData)
        }
        val workflowService = mock<WorkflowQueryServiceImpl> {
            on { get<Any>(any()) }.doReturn(workflow as Workflow<Any>)
        }
        val workflowUpdateService = mock<WorkflowUpdateService> {}
        val definitionBuilder = JobDefinitionBuilder(
            ParameterDefinitionBuilder(),
            ContinuationBuilder(),
            { _ -> null },
            mock {},
            mutableMapOf()
        )
        val modifyingTestJob = definitionBuilder.build(WorkflowModifyingTestJob())
            .createJob(
                jobIdentifier,
                workflow,
                Instant.now(),
                null,
                JobStatus.Scheduled
            )

        val jobService = mock<JobServiceImpl> {
            on {
                getJobOrNull<Any>(
                    any(),
                    any()
                )
            }.doReturn(modifyingTestJob)
            on {
                setStatus(
                    any(),
                    any()
                )
            }.doReturn(modifyingTestJob)
        }
        val persistedWorkflowCaptor = argumentCaptor<Workflow<Any>> { }
        val continuationService = mock<ContinuationService> {}

        val schedulingCallback = JobSchedulingCallback(
            workflowService,
            workflowUpdateService,
            jobService,
            continuationService,
            InterceptedInvocation.empty()
        )

        // Act
        schedulingCallback.onScheduled(testRef)

        // Assert
        verify(
            workflowUpdateService,
            times(1)
        )
            .saveChanges(persistedWorkflowCaptor.capture())
        val persistedWorkflow = persistedWorkflowCaptor.firstValue
        assertThat((persistedWorkflow.model as ModifiableWorkflowData).modifiedValue).isTrue()
    }
}

data class ModifiableWorkflowData(
    var modifiedValue: Boolean,
)

class WorkflowModifyingTestJob {
    fun execute(workflow: Workflow<ModifiableWorkflowData>) {
        workflow.model.modifiedValue = true
    }
}

