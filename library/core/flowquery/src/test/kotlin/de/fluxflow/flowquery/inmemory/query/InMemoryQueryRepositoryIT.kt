package de.fluxflow.flowquery.inmemory.query

import de.fluxflow.flowquery.expression.ExpressionExtensions.Comparisons.isEqual
import de.fluxflow.flowquery.expression.ExpressionExtensions.Comparisons.isLessThan
import de.fluxflow.flowquery.expression.ExpressionExtensions.Logical.not
import de.fluxflow.flowquery.inmemory.expression.compilation.InMemoryCompiler
import de.fluxflow.flowquery.query.sorting.Sort.Companion.desc
import de.fluxflow.flowquery.repository.NonProjectingRepository.Companion.find
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


    @Test
    fun `find should support lessThan filter`() {
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
                get(TestClass::someOtherProperty)
                    .isLessThan(3)
            }
        }

        // Assert
        assertThat(
            result.items.map { it.someOtherProperty }
        ).containsExactlyInAnyOrder(1, 2)
    }


    private data class TestClass(
        val someProperty: String,
        val someOtherProperty: Int
    )
}