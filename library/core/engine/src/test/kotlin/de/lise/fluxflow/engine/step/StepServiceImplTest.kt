package de.lise.fluxflow.engine.step

import de.fluxflow.flowquery.mapper.query.QueryMapper
import de.lise.fluxflow.api.event.EventService
import de.lise.fluxflow.api.state.ChangeDetector
import de.lise.fluxflow.api.step.InvokableStepDefinition
import de.lise.fluxflow.api.step.StepDefinition
import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.step.query.StepQueryable
import de.lise.fluxflow.api.step.stateful.StepActivationException
import de.lise.fluxflow.api.versioning.DefaultCompatibilityTester
import de.lise.fluxflow.api.versioning.NoVersion
import de.lise.fluxflow.api.versioning.VersionCompatibility
import de.lise.fluxflow.api.versioning.VersionRecorder
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowQueryService
import de.lise.fluxflow.engine.continuation.ContinuationService
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.StepPersistence
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.UnknownTypeException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class StepServiceImplTest {
    @Test
    fun `create should not persist a step or its definition when the kind is unregistered`() {
        val kind = "unregistered-step"
        val recorder = mock<VersionRecorder<StepDefinition>>()
        val persistence = mock<StepPersistence> {
            on { randomId() } doReturn "new-step"
        }
        val events = mock<EventService>()
        val resolver = mock<StepTypeResolver> {
            on { resolveType(StepKind(kind)) } doThrow UnknownTypeException(TypeRole.STEP, kind)
        }
        val definition = mock<StepDefinition> {
            on { this.kind } doReturn StepKind(kind)
            on { version } doReturn NoVersion()
            on { metadata } doReturn emptyMap()
        }
        val invokable = mock<InvokableStepDefinition> {
            on { this.definition } doReturn definition
        }
        val service = StepServiceImpl(
            persistence,
            DefaultStepActivationService(
                mock(),
                mock(),
                resolver,
                VersionCompatibility.Unknown,
                DefaultCompatibilityTester(),
            ),
            events,
            mock<ContinuationService>(),
            mock<ChangeDetector<StepData>>(),
            recorder,
            mock<WorkflowQueryService>(),
            false,
            VersionCompatibility.Unknown,
            DefaultCompatibilityTester(),
            mock<QueryMapper<StepQueryable, StepData>>(),
        )
        val workflow = mock<Workflow<Any>> {
            on { identifier } doReturn WorkflowIdentifier("workflow-id")
        }

        val failure = catchThrowable {
            service.create(workflow, invokable)
        }

        assertThat(failure)
            .isExactlyInstanceOf(StepActivationException::class.java)
            .hasMessage("Unable to activate step #new-step with kind '$kind'")
        verifyNoInteractions(recorder)
        verifyNoInteractions(events)
        verify(persistence, never()).create(any())
    }
}
