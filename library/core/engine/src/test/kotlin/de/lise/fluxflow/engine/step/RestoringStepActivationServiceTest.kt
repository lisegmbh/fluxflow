package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepDefinition
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.step.stateful.StepActivationException
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.engine.step.definition.StepDefinitionService
import de.lise.fluxflow.persistence.step.StepData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*

class RestoringStepActivationServiceTest {
    @Test
    fun `toStepDefinition should redirect all invocations to the base activation service`() {
        // Arrange
        val stepDefinition = mock<StepDefinition> {}
        val baseService = mock<StepActivationService> {
            on { toStepDefinition(any()) } doReturn stepDefinition
        }
        val testObject = Any()

        val restoringStepActivationService = RestoringStepActivationService(
            baseService,
            mock<StepDefinitionService> {},
            false
        )

        // Act
        val result = restoringStepActivationService.toStepDefinition(testObject)

        // Assert
        assertThat(result).isSameAs(result)
        verify(baseService, times(1)).toStepDefinition(testObject)
    }

    @Test
    fun `activate should use the base activation service and return it's result, if no exception occurrs`() {
        // Arrange
        val activatedStep = mock<Step> {}
        val workflow = mock<Workflow<Any>> {}
        val stepData = mock<StepData>()
        val baseService = mock<StepActivationService> {
            on { activate(workflow, stepData) } doReturn activatedStep
        }

        val restoringStepActivationService = RestoringStepActivationService(
            baseService,
            mock<StepDefinitionService> {},
            false
        )

        // Act
        val result = restoringStepActivationService.activate(workflow, stepData)

        // Assert
        verify(baseService, times(1)).activate(workflow, stepData)
        assertThat(result).isSameAs(activatedStep)
    }

    @Test
    fun `activate should bubble non StepActivationExceptions if handleAnyException is set to false`() {
        // Arrange
        val workflow = mock<Workflow<Any>> {}
        val stepData = mock<StepData>()
        val exception = NoSuchElementException("I am not a StepActivationException.")
        val baseService = mock<StepActivationService> {
            on { activate(workflow, stepData) } doThrow exception
        }

        val restoringStepActivationService = RestoringStepActivationService(
            baseService,
            mock<StepDefinitionService> {},
            false
        )

        // Act & Assert
        val thrownException = assertThrows<NoSuchElementException> {
            restoringStepActivationService.activate(workflow, stepData)
        }
        assertThat(thrownException).isSameAs(exception)
    }

    @Test
    fun `activate should try to restore steps if a StepActivationException is thrown`() {
        // Arrange
        val workflow = mock<Workflow<Any>> {}
        val stepData = mock<StepData> {
            on { kind } doReturn "kind"
            on { id } doReturn "id"
        }
        val exception = StepActivationException("I am a StepActivationException.")
        val baseService = mock<StepActivationService> {
            on { activate(workflow, stepData) } doThrow exception
        }
        val restoredStep = mock<Step> {
            on { identifier } doReturn StepIdentifier("id")
        }
        val restoredStepDefinition = mock<StepDefinition> {
            on { createStep(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()) } doReturn restoredStep
        }
        val stepDefinitionService = mock<StepDefinitionService> {
                on { createRestoredStepDefinition(any(), any(), any()) } doReturn restoredStepDefinition
        }

        val restoringStepActivationService = RestoringStepActivationService(
            baseService,
            stepDefinitionService,
            false
        )

        // Act
        val result = restoringStepActivationService.activate(workflow, stepData)

        // Assert
        assertThat(result).isSameAs(restoredStep)
        verify(stepDefinitionService, times(1)).createRestoredStepDefinition(
            eq(StepKind("kind")),
            any(),
            same(stepData)
        )
    }

    @Test
    fun `activate should try to restore steps if any exception is thrown and handleAnyException is set to true`() {
        // Arrange
        val workflow = mock<Workflow<Any>> {}
        val stepData = mock<StepData> {
            on { kind } doReturn "kind"
            on { id } doReturn "id"
        }
        val exception = NoSuchElementException("I am not an StepActivationException.")
        val baseService = mock<StepActivationService> {
            on { activate(workflow, stepData) } doThrow exception
        }
        val restoredStep = mock<Step> {
            on { identifier } doReturn StepIdentifier("id")
        }
        val restoredStepDefinition = mock<StepDefinition> {
            on { createStep(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()) } doReturn restoredStep
        }
        val stepDefinitionService = mock<StepDefinitionService> {
            on { createRestoredStepDefinition(any(), any(), any()) } doReturn restoredStepDefinition
        }

        val restoringStepActivationService = RestoringStepActivationService(
            baseService,
            stepDefinitionService,
            true
        )

        // Act
        val result = restoringStepActivationService.activate(workflow, stepData)

        // Assert
        assertThat(result).isSameAs(restoredStep)
        verify(stepDefinitionService, times(1)).createRestoredStepDefinition(
            eq(StepKind("kind")),
            any(),
            same(stepData)
        )
    }

    @Test
    fun `activate should throw a wrapping StepActivationException, if there is no persisted step defnition`() {
        // Arrange
        val workflow = mock<Workflow<Any>> {}
        val stepData = mock<StepData> {
            on { kind } doReturn "kind"
            on { id } doReturn "id"
        }
        val exception = StepActivationException("I am a StepActivationException.")
        val baseService = mock<StepActivationService> {
            on { activate(workflow, stepData) } doThrow exception
        }
        val stepDefinitionService = mock<StepDefinitionService> {
            on { createRestoredStepDefinition(any(), any(), any()) } doReturn null
        }

        val restoringStepActivationService = RestoringStepActivationService(
            baseService,
            stepDefinitionService,
            false
        )

        // Act & Assert
        val thrownException = assertThrows<StepActivationException> {
            restoringStepActivationService.activate(workflow, stepData)
        }
        assertThat(thrownException.cause).isSameAs(exception)
        assertThat(thrownException.message).contains("there is no persisted record of its definition")
    }
}