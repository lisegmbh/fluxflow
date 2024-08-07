package de.lise.fluxflow.stereotyped.step.action

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolution
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import de.lise.fluxflow.stereotyped.continuation.ContinuationBuilder
import de.lise.fluxflow.stereotyped.continuation.ContinuationConverter
import de.lise.fluxflow.stereotyped.metadata.MetadataBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Clock
import kotlin.reflect.KFunction

@Suppress("UNUSED_PARAMETER")
class ActionDefinitionBuilderTest {
    @Test
    fun `build should use the supplied metadata builder to obtain an action's metadata`() {
        // Arrange
        val continuationBuilder = mock<ContinuationBuilder> {
            on { createResultConverter<Any?>(any(), any(), any()) } doReturn mock<ContinuationConverter<Any?>> {}
        }
        val metadataBuilder = mock<MetadataBuilder> {}
        val actionFunctionResolver = mock<ActionFunctionResolver> {}
        val actionDefinitionBuilder = ActionDefinitionBuilder(
            continuationBuilder,
            metadataBuilder,
            actionFunctionResolver
        )

        // Act
        actionDefinitionBuilder.build<TestClass>(TestClass::someFunction, false)

        // Assert
        verify(metadataBuilder, times(1)).build(TestClass::someFunction)
    }

    @Test
    fun `the action definition returned by build should contain the metadata created by the metadata builder`() {
        // Arrange
        val continuationBuilder = mock<ContinuationBuilder> {
            on { createResultConverter<Any?>(any(), any(), any()) } doReturn mock<ContinuationConverter<Any?>> {}
        }
        val actionFunctionResolver = mock<ActionFunctionResolver> {}
        val testMetadata = emptyMap<String, Any>()
        val metadataBuilder = mock<MetadataBuilder> {
            on { build(any<KFunction<*>>()) } doReturn testMetadata
        }
        val actionDefinitionBuilder = ActionDefinitionBuilder(
            continuationBuilder,
            metadataBuilder,
            actionFunctionResolver
        )

        // Act
        val actionDefinition = actionDefinitionBuilder.build<TestClass>(
            TestClass::someFunction, false
        )!!.invoke(TestClass())

        // Assert
        assertThat(actionDefinition.metadata).isSameAs(testMetadata)
    }

    @Test
    fun `build should support action functions requiring additional parameters if annotated explicitly`() {
        // Arrange
        val actionDefinitionBuilder = ActionDefinitionBuilder(
            mock { },
            mock { },
            mock { }
        )

        // Act
        val stepDefinitionFunction = actionDefinitionBuilder.build<TestClassWithExplicitlyAnnotatedActions>(
            TestClassWithExplicitlyAnnotatedActions::someFunctionWithDependency, false
        )

        // Assert
        assertThat(stepDefinitionFunction).isNotNull
    }

    @Test
    fun `actions built for functions requiring additional parameters should be executable`() {
        // Arrange
        val step = mock<Step> { }
        val clock = mock<Clock> { }
        val parameterResolution = mock<ParameterResolution> {
            on { get() } doReturn clock
        }
        val parameterResolver = mock<ParameterResolver> {
            on { resolveParameter(any()) } doReturn parameterResolution
        }
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
        val actionDefinitionBuilder = ActionDefinitionBuilder(
            continuationBuilder,
            mock { },
            ActionFunctionResolverImpl(parameterResolver)
        )
        val stepDefinitionFunction = actionDefinitionBuilder.build<TestClassWithExplicitlyAnnotatedActions>(
            TestClassWithExplicitlyAnnotatedActions::someFunctionWithDependency, false
        )
        val stepInstance = TestClassWithExplicitlyAnnotatedActions()
        val actionDefinition = stepDefinitionFunction!!.invoke(stepInstance)
        val action = actionDefinition.createAction(step)

        // Act
        action.execute()

        // Assert
        assertThat(stepInstance.receivedParam).isSameAs(clock)
    }

    @Test
    fun `build should not support action functions requiring additional parameters if not annotated explicitly`() {
        // Arrange
        val actionDefinitionBuilder = ActionDefinitionBuilder(
            mock { },
            mock { },
            mock { }
        )

        // Act
        val stepDefinitionFunction = actionDefinitionBuilder.build<TestClass>(
            TestClass::someFunctionWithDependency, false
        )

        // Assert
        assertThat(stepDefinitionFunction).isNull()
    }

    class TestClass {
        fun someFunction() {}
        fun someFunctionWithDependency(dependency: Clock) {}
    }

    class TestClassWithExplicitlyAnnotatedActions {
        var receivedParam: Clock? = null
        @Action
        fun someFunctionWithDependency(dependency: Clock) {
            receivedParam = dependency
        }
    }
}