package de.lise.fluxflow.query.filter

import de.lise.fluxflow.query.inmemory.filter.InMemoryEqualFilter
import de.lise.fluxflow.query.inmemory.filter.InMemoryNullablePropertyFilter
import de.lise.fluxflow.query.inmemory.filter.InMemoryPropertyFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InMemoryNullablePropertyFilterTest {

    @Test
    fun `nullableProperty filter should ignore objects with null values`() {
        // Arrange
        val innerFilter = InMemoryEqualFilter("expectedValue")

        val nullablePropertyFilter = InMemoryNullablePropertyFilter(
            TestObjectWithNestedNullableObject::nested,
            InMemoryPropertyFilter(
                TestNestedObject::value,
                innerFilter
            )
        )

        // Act
        val result = nullablePropertyFilter.toPredicate()

        // Assert
        val withValue = TestObjectWithNestedNullableObject(TestNestedObject("expectedValue"))
        val withWrongValue = TestObjectWithNestedNullableObject(TestNestedObject("wrongValue"))
        val withNullValue = TestObjectWithNestedNullableObject(null)

        assertThat(result.test(withValue)).isTrue()
        assertThat(result.test(withWrongValue)).isFalse()
        assertThat(result.test(withNullValue)).isFalse()
    }

    private data class TestObjectWithNestedNullableObject(val nested: TestNestedObject?)

    private data class TestNestedObject(val value: String)
}
