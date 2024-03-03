package de.lise.fluxflow.stereotyped.step

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import de.lise.fluxflow.stereotyped.continuation.ContinuationBuilder
import de.lise.fluxflow.stereotyped.continuation.ContinuationConverter
import de.lise.fluxflow.stereotyped.step.action.ActionDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.automation.Automated
import de.lise.fluxflow.stereotyped.step.automation.AutomationDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.automation.OnCreated
import de.lise.fluxflow.stereotyped.step.automation.Trigger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@Suppress("unused")
class StepDefinitionBuilderIT {
    @Test
    fun `build should construct the correct actions`() {
        // Arrange
        val actionDefinitionBuilder = mock<ActionDefinitionBuilder> {}
        val continuationBuilder = mock<ContinuationBuilder> {
            on {
                createResultConverter<Any>(
                    any(),
                    any(),
                    any()
                )
            } doReturn ContinuationConverter<Any> {
                Continuation.none()
            }
        }
        val parameterResolver = mock<ParameterResolver> {}
        val builder = StepDefinitionBuilder(
            actionDefinitionBuilder,
            mock {},
            mock {},
            AutomationDefinitionBuilder(continuationBuilder, parameterResolver),
            mutableMapOf()
        )
        val stepModel = TestClassWithAutomation()
        val step = mock<Step> {}

        // Act
        val stepDefinition = builder.build(stepModel)

        // Assert
        assertThat(stepDefinition.onCreatedAutomations).hasSize(2)
        stepDefinition.onCreatedAutomations
            .map { it.createAutomation(step) }
            .forEach { it.execute() }

        assertThat(stepModel.onStartedHasRun).isTrue
        assertThat(stepModel.automatedHasRun).isTrue
    }

    class TestClassWithAutomation {
        var automatedHasRun = false
        var onStartedHasRun = false

        @Automated(Trigger.OnCreated)
        fun `automated function`() {
            automatedHasRun = true
        }

        @OnCreated
        fun `onStarted function`() {
            onStartedHasRun = true
        }
    }
}