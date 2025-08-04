package de.lise.fluxflow.stereotyped.workflow.action

import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolution
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import de.lise.fluxflow.stereotyped.continuation.ContinuationBuilder
import de.lise.fluxflow.stereotyped.continuation.ContinuationConverter
import de.lise.fluxflow.stereotyped.metadata.MetadataBuilder
import de.lise.fluxflow.stereotyped.step.action.Action
import de.lise.fluxflow.stereotyped.step.action.ActionKindInspector.Companion.actionKind
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.*
import java.time.Clock
import kotlin.reflect.KFunction

class WorkflowActionDefinitionBuilderTest {
    @Test
    fun `build should use the supplied metadata builder to obtain an action's metadata`() {
        // Arrange
        val continuationBuilder = mock<ContinuationBuilder> { }
        val metadata = mapOf("metadata-key" to "metadata-value")
        val metadataBuilder = mock<MetadataBuilder> {
            on { build(any<KFunction<*>>()) } doReturn metadata
        }
        val actionFunctionResolver = mock<WorkflowActionFunctionResolver> { }

        val actionDefinitionBuilder = WorkflowActionDefinitionBuilder(
            metadataBuilder,
            continuationBuilder,
            actionFunctionResolver
        )

        // Act
        val result = actionDefinitionBuilder.build(TestClass::class)

        // Assert
        val resultingActionDefinition = result.first{ it.kind ==  TestClass::someWorkflowAction.actionKind }
        assertThat(resultingActionDefinition.metadata).isEqualTo(metadata)
        verify(metadataBuilder, times(1)).build(TestClass::someWorkflowAction)
    }


    @Test
    fun `build should ignore functions not annotated with @Action`() {
        // Arrange
        val continuationBuilder = mock<ContinuationBuilder> { }
        val metadataBuilder = mock<MetadataBuilder> { }
        val actionFunctionResolver = mock<WorkflowActionFunctionResolver> { }

        val actionDefinitionBuilder = WorkflowActionDefinitionBuilder(
            metadataBuilder,
            continuationBuilder,
            actionFunctionResolver
        )

        // Act
        val result = actionDefinitionBuilder.build(TestClass::class)

        // Assert
        val definitionForSomeOtherFunction = result.firstOrNull {
            it.kind == TestClass::someOtherFunction.actionKind
        }
        assertThat(definitionForSomeOtherFunction).isNull()
    }

    @Test
    fun `actions built for functions requiring additional parameters should be executable`() {
        // Arrange
        val continuationConverter = mock<ContinuationConverter<Any>> {}
        val continuationBuilder = mock<ContinuationBuilder> {
            on {
                createResultConverter<Any>(
                    any(),
                    any(),
                    any()
                )
            } doReturn continuationConverter
        }
        val metadataBuilder = mock<MetadataBuilder> { }
        val clock = mock<Clock> {  }
        val parameterResolution = mock<ParameterResolution> {
            on { get() } doReturn clock
        }
        val parameterResolver = mock<ParameterResolver> {
            on { resolveParameter(any()) } doReturn parameterResolution
        }
        val actionFunctionResolver = WorkflowActionFunctionResolverImpl(parameterResolver)

        val actionDefinitionBuilder = WorkflowActionDefinitionBuilder(
            metadataBuilder,
            continuationBuilder,
            actionFunctionResolver
        )

        val workflowModel = TestClass()
        val workflow = mock<Workflow<TestClass>> {
            on { model } doReturn workflowModel
        }

        // Act
        val result = actionDefinitionBuilder.build(TestClass::class)
        val actionDefinition = result.first { it.kind == TestClass::someWorkflowRequiringAParameter.actionKind }
        val action = actionDefinition.createAction(workflow)


        // Assert
        assertDoesNotThrow {
            action.execute()
        }
        verify(parameterResolution).get()
        assertThat(workflowModel.receivedClock).isSameAs(clock)
    }

    class TestClass {
        var receivedClock: Clock? = null

        @Action
        fun someWorkflowAction() {}

        @Suppress("unused")
        fun someOtherFunction() {}

        @Action
        fun someWorkflowRequiringAParameter(c: Clock) {
            receivedClock = c
        }
    }
}