package de.lise.fluxflow.stereotyped.step.automation

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.continuation.StatusBehavior
import de.lise.fluxflow.reflection.activation.parameter.FunctionParameter
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolution
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import de.lise.fluxflow.stereotyped.continuation.ContinuationBuilder
import de.lise.fluxflow.stereotyped.step.ReflectedStatefulStep
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import java.time.Clock

class AutomationDefinitionBuilderTest {
    @Test
    fun `build should preserve the continuation's status behavior`() {
        // Arrange
        val continuationBuilder = ContinuationBuilder()
        val parameterResolver = mock<ParameterResolver> { }
        val testClass = TestClass()
        val step = mock<ReflectedStatefulStep> {
            on { instance } doReturn testClass
        }
        val automationDefinitionBuilder = AutomationDefinitionBuilder(
            continuationBuilder,
            parameterResolver
        )

        // Act
        val automationDefinition = automationDefinitionBuilder.build<TestClass>(
            TestClass::automaticallyCompleteStep
        )[Trigger.OnCreated]!!
        val automation = automationDefinition.createAutomation(step)
        val continuation = automation.execute()

        // Assert
        assertThat(continuation.statusBehavior).isEqualTo(StatusBehavior.Complete)
    }

    @Test
    fun `automation functions requiring additional parameters should throw an exception if they can not be resolved`() {
        // Arrange
        val continuationBuilder = ContinuationBuilder()
        val parameterResolver = mock<ParameterResolver> { }
        val testClass = TestClassWithDependency()
        val step = mock<ReflectedStatefulStep> {
            on { instance } doReturn testClass
        }
        val automationDefinitionBuilder = AutomationDefinitionBuilder(
            continuationBuilder,
            parameterResolver
        )

        val automationDefinition = automationDefinitionBuilder.build<TestClassWithDependency>(
            TestClassWithDependency::requireDependency
        )[Trigger.OnCreated]!!
        val automation = automationDefinition.createAutomation(step)

        // Act & Assert
        assertThrows<AutomationConfigurationException> {
            automation.execute()
        }
    }

    @Test
    fun `automation functions requiring additional parameters should be successfully executable, if all parameters could be resolved`() {
        // Arrange
        val continuationBuilder = ContinuationBuilder()
        val clockParameter = FunctionParameter(
            TestClassWithDependency::requireDependency,
            TestClassWithDependency::requireDependency.parameters.single{ it.name == "clock"}
        )
        val clock = mock<Clock> {}
        val parameterResolution = mock<ParameterResolution> {
            on { get() } doReturn clock
        }
        val parameterResolver = mock<ParameterResolver> {
            on { resolveParameter(eq(clockParameter)) } doReturn parameterResolution
        }
        val testClass = TestClassWithDependency()
        val step = mock<ReflectedStatefulStep> {
            on { instance } doReturn testClass
        }
        val automationDefinitionBuilder = AutomationDefinitionBuilder(
            continuationBuilder,
            parameterResolver
        )
        val automationDefinition = automationDefinitionBuilder.build<TestClassWithDependency>(
            TestClassWithDependency::requireDependency
        )[Trigger.OnCreated]!!
        val automation = automationDefinition.createAutomation(step)

        // Act
        assertDoesNotThrow {
            automation.execute()
        }
        assertThat(testClass.receivedClock).isSameAs(clock)
    }

    class TestClass {
        @OnCreated
        fun automaticallyCompleteStep(): Continuation<*> {
            return Continuation.none().withStatusBehavior(StatusBehavior.Complete)
        }
    }

    class TestClassWithDependency {
        var receivedClock: Clock? = null

        @OnCreated
        fun requireDependency(clock: Clock) {
            receivedClock = clock
        }
    }
}