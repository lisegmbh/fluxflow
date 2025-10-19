package de.fluxflow.flowquery.inmemory.query

import de.fluxflow.flowquery.expression.not
import de.fluxflow.flowquery.inmemory.InMemoryCompiler
import de.fluxflow.flowquery.query.sorting.Sort.Companion.desc
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InMemoryQueryRepositoryIT {
    @Test
    fun `find should support sorting`() {
        // Arrange
        val data = listOf(
            TestClass("a", 1),
            TestClass("b", 2),
            TestClass("c", 3)
        )
        val repo = InMemoryQueryRepository(
            InMemoryCompiler()
        ) { data }

        // Act
        val result = repo.find {
            where {
                get(TestClass::someProperty)
                    .isEqual("c")
                    .not()
            }.sort {
                get(TestClass::someProperty).desc()
            }
        }

        // Assert
        assertThat(
            result.items.map {
                it.someProperty
            }
        ).containsExactly("b", "a")
    }


    private data class TestClass(
        val someProperty: String,
        val someOtherProperty: Int
    )
}