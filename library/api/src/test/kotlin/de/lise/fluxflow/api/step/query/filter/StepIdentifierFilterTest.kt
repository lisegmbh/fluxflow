package de.lise.fluxflow.api.step.query.filter

import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.query.filter.AnyOfFilter
import de.lise.fluxflow.query.filter.EqualFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StepIdentifierFilterTest {
    @Test
    fun `eq should create a filter testing for an equal value`() {
        // Arrange
        val testIdentifier = StepIdentifier("foo-bar")

        // Act
        val filter = StepIdentifierFilter.eq(testIdentifier)

        // Assert
        assertThat(filter.value).isInstanceOf(EqualFilter::class.java)
        val valueFilter = filter.value as EqualFilter<String>
        assertThat(valueFilter.expectedValue).isEqualTo(testIdentifier.value)
    }

    @Test
    fun `anyOf should create a filter testing for a value contained within the given list`() {
        // Arrange
        val testIdentifiers = listOf(
            StepIdentifier("a"),
            StepIdentifier("b")
        )

        // Act
        val filter = StepIdentifierFilter.anyOf(testIdentifiers)

        // Assert
        assertThat(filter.value).isInstanceOf(AnyOfFilter::class.java)
        val anyOfFilter = filter.value as AnyOfFilter<String>
        assertThat(anyOfFilter.anyOfValues).containsExactlyInAnyOrderElementsOf(
            testIdentifiers.map { it.value }
        )
    }
}