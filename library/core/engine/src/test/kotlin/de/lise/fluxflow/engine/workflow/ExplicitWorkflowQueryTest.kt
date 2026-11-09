package de.lise.fluxflow.engine.workflow

import de.fluxflow.flowquery.mapper.query.QueryMapper
import de.fluxflow.flowquery.query.FlowQuery
import de.fluxflow.flowquery.query.QueryConstraint
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.api.versioning.VersionCompatibility
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.query.StepQueryable
import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.api.job.query.JobQueryable
import de.lise.fluxflow.engine.step.StepServiceImpl
import de.lise.fluxflow.engine.step.StepActivationService
import de.lise.fluxflow.engine.job.JobServiceImpl
import de.lise.fluxflow.engine.job.JobActivationService
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.StepPersistence
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.api.workflow.flowquery.WorkflowQueryable
import de.lise.fluxflow.persistence.workflow.WorkflowData
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import de.lise.fluxflow.query.pagination.Page
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.kotlin.*

class ExplicitWorkflowQueryTest {
    private object Restriction : QueryConstraint

    @Test
    fun `service composition preserves the explicit constraint through query mapping`() {
        val input = FlowQuery.of<WorkflowQueryable<*>>()
        val mapped = FlowQuery.of<WorkflowData>()
        val mapper = mock<QueryMapper<WorkflowQueryable<*>, WorkflowData>>()
        whenever(mapper.map<WorkflowData>(input)).thenReturn(mapped)
        val persistence = mock<WorkflowPersistence>()
        whenever(persistence.findAll(mapped, Restriction)).thenReturn(
            Page(emptyList(), 2, 0, 0, 0, true, true))
        val delegate = WorkflowQueryServiceImpl(persistence, mock(), mapper)
        val service = WorkflowServiceImpl(mock(), delegate, mock(), mock())
        assertThat(service.findAll(input, Restriction).items).isEmpty()
        verify(persistence).findAll(same(mapped), same(Restriction))
        verify(persistence, never()).findAll(any<FlowQuery<WorkflowData, WorkflowData>>())
    }

    @Test
    fun `an existing adapter rejects an explicit constraint instead of calling its ordinary overload`() {
        val persistence = mock<WorkflowPersistence>(defaultAnswer = Answers.CALLS_REAL_METHODS)
        assertThatThrownBy { persistence.findAll(FlowQuery.of<WorkflowData>(), Restriction) }
            .isInstanceOf(UnsupportedOperationException::class.java)
        verify(persistence, never()).findAll(any<FlowQuery<WorkflowData, WorkflowData>>())
    }

    @Test
    fun `step searches still load workflows excluded by a separate explicit query`() {
        val (workflows, workflow) = workflowServiceWithExcludedWorkflow()
        val data = mock<StepData> { on { workflowId } doReturn "hidden" }
        val persistence = mock<StepPersistence>()
        whenever(persistence.findAll(any<FlowQuery<StepData, StepData>>())).thenReturn(Page.unpaged(listOf(data)))
        val mapper = mock<QueryMapper<StepQueryable, StepData>>()
        whenever(mapper.map<StepData>(any<FlowQuery<StepQueryable, StepQueryable>>())).thenReturn(FlowQuery.of())
        val activation = mock<StepActivationService>()
        val step = mock<Step>()
        whenever(activation.activateFromPersistence(workflow, data)).thenReturn(step)
        val service = StepServiceImpl(persistence, activation, mock(), mock(), mock(), mock(), workflows,
            false, VersionCompatibility.Compatible, mock(), mapper)
        assertThat(service.findAll(FlowQuery.of<StepQueryable>()).items).containsExactly(step)
        verify(activation).activateFromPersistence(same(workflow), same(data))
    }

    @Test
    fun `job searches still load workflows excluded by a separate explicit query`() {
        val (workflows, workflow) = workflowServiceWithExcludedWorkflow()
        val data = mock<JobData> { on { workflowId } doReturn "hidden" }
        val persistence = mock<JobPersistence>()
        whenever(persistence.findAll(any<FlowQuery<JobData, JobData>>())).thenReturn(Page.unpaged(listOf(data)))
        val mapper = mock<QueryMapper<JobQueryable, JobData>>()
        whenever(mapper.map<JobData>(any<FlowQuery<JobQueryable, JobQueryable>>())).thenReturn(FlowQuery.of())
        val activation = mock<JobActivationService>()
        val job = mock<Job>()
        whenever(activation.activate(workflow, data)).thenReturn(job)
        val service = JobServiceImpl(activation, persistence, mock(), workflows, mapper)
        assertThat(service.findAll(FlowQuery.of<JobQueryable>()).items).containsExactly(job)
        verify(activation).activate(same(workflow), same(data))
    }

    private fun workflowServiceWithExcludedWorkflow(): Pair<WorkflowService, Workflow<Any>> {
        val data = mock<WorkflowData>()
        val workflow = mock<Workflow<Any>> { on { identifier } doReturn WorkflowIdentifier("hidden") }
        val persistence = mock<WorkflowPersistence>()
        whenever(persistence.findAll(any<FlowQuery<WorkflowData, WorkflowData>>()))
            .thenReturn(Page.unpaged(listOf(data)))
        whenever(persistence.findAll(any<FlowQuery<WorkflowData, WorkflowData>>(), same(Restriction)))
            .thenReturn(Page.unpaged(emptyList()))
        val activation = mock<WorkflowActivationService>()
        whenever(activation.activate<Any>(data)).thenReturn(workflow)
        val mapper = mock<QueryMapper<WorkflowQueryable<*>, WorkflowData>>()
        whenever(mapper.map<WorkflowData>(any<FlowQuery<WorkflowQueryable<*>, WorkflowQueryable<*>>>()))
            .thenReturn(FlowQuery.of())
        val service = WorkflowServiceImpl(mock(), WorkflowQueryServiceImpl(persistence, activation, mapper), mock(), mock())
        assertThat(service.findAll(FlowQuery.of<WorkflowQueryable<*>>(), Restriction).items).isEmpty()
        return service to workflow
    }
}
