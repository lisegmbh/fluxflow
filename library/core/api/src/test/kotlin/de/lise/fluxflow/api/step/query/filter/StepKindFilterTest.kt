package de.lise.fluxflow.api.step.query.filter

import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.query.filter.EqualFilter
import de.lise.fluxflow.query.filter.InFilter
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
        assertThat(filter.value).isInstanceOf(InFilter::class.java)
        val inFilter = filter.value as InFilter<String>
        assertThat(inFilter.anyOfValues).containsExactlyInAnyOrderElementsOf(
            testKinds.map { it.value }
        )
    }

}