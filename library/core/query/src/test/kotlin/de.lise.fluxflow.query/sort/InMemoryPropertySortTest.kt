package de.lise.fluxflow.query.sort

import de.lise.fluxflow.query.inmemory.sort.InMemoryDefaultSort
import de.lise.fluxflow.query.inmemory.sort.InMemoryPropertySort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InMemoryPropertySortTest {
    @Test
    fun `property sort should correctly order objects by property`() {
        // Arrange
        val sort = InMemoryPropertySort(
            TestObject::value,
            InMemoryDefaultSort(Direction.Ascending)
        )

        val sortedList = listOf(
            TestObject(1),
            TestObject(2),
            TestObject(3)
        )

        // Act & Assert
        repeat(10) {
            val shuffled = sortedList.shuffled()
            val result = shuffled.sortedWith(sort.toComparator())

            assertThat(result).containsExactly(*sortedList.toTypedArray())
        }
    }
}

private data class TestObject(val value: Number)
