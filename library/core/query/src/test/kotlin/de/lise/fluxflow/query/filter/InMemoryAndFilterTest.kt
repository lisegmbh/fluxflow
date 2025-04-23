package de.lise.fluxflow.query.filter

import de.lise.fluxflow.query.inmemory.filter.InMemoryAndFilter
import de.lise.fluxflow.query.inmemory.filter.InMemoryContainsFilter
import org.junit.jupiter.api.Test

class InMemoryAndFilterTest {
    @Test
    fun `from AndFilter constructor should combine inner filters`() {
        // Arrange
        val innerFilter = ContainsFilter("ExpectedValue", false)
        val innerFilter2 = ContainsFilter("AnotherExpectedValue", false)
        val andFilter = InMemoryAndFilter(
            AndFilter(
                listOf(innerFilter, innerFilter2)
            )
        )

        // Act
        val result = andFilter.toPredicate()

        // Assert
        assert(result.test("ExpectedValue and AnotherExpectedValue"))
    }

    @Test
    fun `from list constructor should combine inner filters`() {
        // Arrange
        val innerFilter = InMemoryContainsFilter("ExpectedValue")
        val innerFilter2 = InMemoryContainsFilter("AnotherExpectedValue")
        val andFilter = InMemoryAndFilter(
            listOf(innerFilter, innerFilter2)
        )

        // Act
        val result = andFilter.toPredicate()

        // Assert
        assert(result.test("ExpectedValue and AnotherExpectedValue"))
    }
}