package de.fluxflow.flowquery.inmemory.query

import de.fluxflow.flowquery.expression.ExpressionExtensions.Types.asType
import de.fluxflow.flowquery.expression.compilation.StaticSubclassProvider
import de.fluxflow.flowquery.inmemory.expression.compilation.InMemoryCompiler
import de.fluxflow.flowquery.query.sorting.Sort.Companion.asc
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InMemoryAsTypeSortIT {
    @Test
    fun `sort should use casted property for matching types and null for others`() {
        // Arrange
        val data = listOf(
            TestRoot(TypedModel("B")),
            TestRoot(OtherModel("Z")),
            TestRoot(TypedModel("A")),
            TestRoot(BaseModel()),
        )
        val repo = InMemoryQueryRepository(
            InMemoryCompiler(
                StaticSubclassProvider(
                    mapOf(
                        TypedModel::class to setOf(TypedModel::class.java),
                    )
                )
            )
        ) { data }

        // Act
        val result = repo.find {
            sort {
                get(TestRoot::model)
                    .asType(TypedModel::class)
                    .get(TypedModel::label)
                    .asc()
            }
        }

        // Assert
        assertThat(result.items).hasSize(4)

        val labels = result.items.map { (it.model as? TypedModel)?.label }
        assertThat(labels).containsExactly(null, null, "A", "B")
    }

    private open class BaseModel

    private class TypedModel(val label: String) : BaseModel()

    private class OtherModel(val label: String) : BaseModel()

    private data class TestRoot(val model: BaseModel)
}
