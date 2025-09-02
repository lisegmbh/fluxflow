package de.lise.fluxflow.query.filter

import de.lise.fluxflow.query.inmemory.filter.InMemoryEqualFilter
import de.lise.fluxflow.query.inmemory.filter.InMemoryNotFilter
import org.junit.jupiter.api.Test

class InMemoryNotFilterTest {
    @Test
    fun `not filter should negate inner filter`() {
        // Arrange
        val innerFilter = InMemoryEqualFilter("NotExpectedValue")
        val notFilter = InMemoryNotFilter(innerFilter)

        // Act
        val result = notFilter.toPredicate()

        // Assert
        assert(!result.test("NotExpectedValue"))
    }
}