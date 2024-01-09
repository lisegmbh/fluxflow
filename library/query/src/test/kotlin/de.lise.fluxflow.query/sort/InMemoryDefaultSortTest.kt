package de.lise.fluxflow.query.sort

import de.lise.fluxflow.query.inmemory.sort.InMemoryDefaultSort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InMemoryDefaultSortTest {
    @Test
    fun `default sort should correctly order strings ascending`() {
        // Arrange
        val sort = InMemoryDefaultSort<String>(Direction.Ascending)

        // Act
        val result = listOf(
            "b",
            "a",
            "c"
        ).sortedWith(sort.toComparator())

        // Assert
        assertThat(result).containsExactly("a", "b", "c")
    }

    @Test
    fun `default sort should correctly order strings descending`() {
        // Arrange
        val sort = InMemoryDefaultSort<String>(Direction.Descending)

        // Act
        val result = listOf(
            "b",
            "a",
            "c"
        ).sortedWith(sort.toComparator())

        // Assert
        assertThat(result).containsExactly("c", "b", "a")
    }

    @Test
    fun `default sort shout throw an exception if elements are not comparable`() {
        // Arrange
        val sort = InMemoryDefaultSort<Any>(Direction.Ascending)

        // Act & Assert
        assertThrows<IncomparableElementsException> {
            listOf(
                3,
                true,
                Any()
            ).sortedWith(sort.toComparator())
        }
    }

}