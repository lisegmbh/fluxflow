package de.lise.fluxflow.query.filter

import de.lise.fluxflow.query.inmemory.filter.InMemoryElemMatchFilter
import de.lise.fluxflow.query.inmemory.filter.InMemoryEqualFilter
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class InMemoryElemMatchFilterTest {
    @Test
    fun `elemMatch filter should apply inner filter to collections`() {
        // Arrange
        val innerFilter = InMemoryEqualFilter(8)
        val elementMatchFilter = InMemoryElemMatchFilter(
            String::length,
            innerFilter
        )

        // Act
        val result = elementMatchFilter.toPredicate()

        // Assert
        assert(result.test(listOf("Workflow", "Step")))
        assertFalse(result.test(listOf("Step", "Data")))
    }
}