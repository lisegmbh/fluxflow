package de.lise.fluxflow.api.step.query.filter

import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.query.filter.AnyOfFilter
import de.lise.fluxflow.query.filter.EqualFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StepKindFilterTest {

    @Test
    fun `eq should return a filter testing for kind value equality`() {
        // Arrange
        val testKind = StepKind("test")

        // Act
        val filter = StepKindFilter.eq(testKind)

        // Assert
        assertThat(filter.value).isInstanceOf(EqualFilter::class.java)
        val equalFilter = filter.value as EqualFilter<String>
        assertThat(equalFilter.expectedValue).isEqualTo(testKind.value)
    }

    @Test
    fun `anyOf should return a filter testing for kind value equality of any given kind`() {
        // Arrange
        val testKinds = listOf(
            StepKind("a"),
            StepKind("b")
        )

        // Act
        val filter = StepKindFilter.anyOf(testKinds)

        // Assert
        assertThat(filter.value).isInstanceOf(AnyOfFilter::class.java)
        val anyOfFilter = filter.value as AnyOfFilter<String>
        assertThat(anyOfFilter.anyOfValues).containsExactlyInAnyOrderElementsOf(
            testKinds.map { it.value }
        )
    }

}