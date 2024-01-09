package de.lise.fluxflow.stereotyped.continuation

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.continuation.NoContinuation
import de.lise.fluxflow.api.continuation.StatusBehavior
import de.lise.fluxflow.api.step.continuation.StepContinuation
import de.lise.fluxflow.stereotyped.step.action.ImplicitStatusBehavior
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class ContinuationConverterTest {
    @Test
    fun `created converter should simply return the value if it is already a continuation`() {
        // Arrange
        val continuationBuilder = ContinuationBuilder()
        val testContinuation = mock<Continuation<Any>> {}

        // Act
        val createdContinuation = continuationBuilder.createResultConverter(
            ImplicitStatusBehavior.Default,
            true,
            TestInterface::functionReturningAContinuation
        ).toContinuation(testContinuation)

        // Assert
        assertThat(createdContinuation).isSameAs(testContinuation)
    }

    @Test
    fun `created converter should return a noop continuation for functions returning void`() {
        // Arrange
        val continuationBuilder = ContinuationBuilder()

        // Act
        val createdContinuation = continuationBuilder.createResultConverter(
            ImplicitStatusBehavior.Default,
            true,
            TestInterface::functionReturningVoid
        ).toContinuation(Unit)

        // Assert
        assertThat(createdContinuation).isInstanceOf(NoContinuation::class.java)
    }

    @Test
    fun `created converter should assume that the returned value represents a step as a fallback`() {
        // Arrange
        val continuationBuilder = ContinuationBuilder()
        val testStepValue = TestStep()

        // Act
        val createdContinuation = continuationBuilder.createResultConverter(
            ImplicitStatusBehavior.Default,
            true,
            TestInterface::functionReturningAnythingElse
        ).toContinuation(testStepValue)

        // Assert
        assertThat(createdContinuation).isInstanceOf(StepContinuation::class.java)
        @Suppress("UNCHECKED_CAST")
        val createdStepContinuation = createdContinuation as StepContinuation<TestStep>
        assertThat(createdStepContinuation.model).isSameAs(testStepValue)
    }

    @Test
    fun `created converter should not force the preserve behavior if the default behavior is used`() {
        // Arrange
        val continuationBuilder = ContinuationBuilder()
        val nonPreservingTestContinuation = Continuation.none().withStatusBehavior(StatusBehavior.Complete)
        val preservingTestContinuation = Continuation.none().withStatusBehavior(StatusBehavior.Preserve)


        // Act
        val createdNonPreservingContinuation = continuationBuilder.createResultConverter(
            ImplicitStatusBehavior.Default,
            true,
            TestInterface::functionReturningAContinuation
        ).toContinuation(nonPreservingTestContinuation)

        val createdPreservingContinuation = continuationBuilder.createResultConverter(
            ImplicitStatusBehavior.Default,
            true,
            TestInterface::functionReturningAContinuation
        ).toContinuation(preservingTestContinuation)

        // Assert
        assertThat(createdNonPreservingContinuation.statusBehavior).isNotEqualTo(StatusBehavior.Preserve)
        assertThat(createdPreservingContinuation.statusBehavior).isEqualTo(StatusBehavior.Preserve)
    }

    @Test
    fun `created converter should always force the preserve behavior if the preserve behavior is used`() {
        // Arrange
        val continuationBuilder = ContinuationBuilder()
        val nonPreservingTestContinuation = Continuation.none().withStatusBehavior(StatusBehavior.Complete)
        val preservingTestContinuation = Continuation.none().withStatusBehavior(StatusBehavior.Preserve)

        // Act
        val createdNonPreservingContinuation = continuationBuilder.createResultConverter(
            ImplicitStatusBehavior.Preserve,
            true,
            TestInterface::functionReturningAContinuation
        ).toContinuation(nonPreservingTestContinuation)

        val createdPreservingContinuation = continuationBuilder.createResultConverter(
            ImplicitStatusBehavior.Preserve,
            true,
            TestInterface::functionReturningAContinuation
        ).toContinuation(preservingTestContinuation)

        // Assert
        assertThat(createdNonPreservingContinuation.statusBehavior).isEqualTo(StatusBehavior.Preserve)
        assertThat(createdPreservingContinuation.statusBehavior).isEqualTo(StatusBehavior.Preserve)
    }

    @Test
    fun `created converter should always force complete behavior if the complete behavior is used`() {
        // Arrange
        val continuationBuilder = ContinuationBuilder()
        val cancelingTestContinuation = Continuation.none().withStatusBehavior(StatusBehavior.Cancel)
        val preservingTestContinuation = Continuation.none().withStatusBehavior(StatusBehavior.Preserve)

        // Act
        val createdCancelingContinuation = continuationBuilder.createResultConverter(
            ImplicitStatusBehavior.Complete,
            true,
            TestInterface::functionReturningAContinuation
        ).toContinuation(cancelingTestContinuation)

        val createdPreservingContinuation = continuationBuilder.createResultConverter(
            ImplicitStatusBehavior.Complete,
            true,
            TestInterface::functionReturningAContinuation
        ).toContinuation(preservingTestContinuation)

        // Assert
        assertThat(createdCancelingContinuation.statusBehavior).isEqualTo(StatusBehavior.Complete)
        assertThat(createdPreservingContinuation.statusBehavior).isEqualTo(StatusBehavior.Complete)
    }

    class TestStep
    interface TestInterface {
        fun functionReturningAContinuation(): Continuation<*>
        fun functionReturningVoid()
        fun functionReturningAnythingElse(): TestStep
    }
}