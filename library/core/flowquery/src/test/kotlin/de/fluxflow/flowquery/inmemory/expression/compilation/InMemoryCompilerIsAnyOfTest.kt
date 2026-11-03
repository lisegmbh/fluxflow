package de.fluxflow.flowquery.inmemory.expression.compilation

import de.fluxflow.flowquery.expression.ExpressionExtensions.Logical.not
import de.fluxflow.flowquery.expression.IsAnyOfOperator
import de.fluxflow.flowquery.expression.PredicateExpression
import de.fluxflow.flowquery.expression.RootExpression
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InMemoryCompilerIsAnyOfTest {

    private data class TestModel(val name: String?, val alias: String? = null)

    private fun <TRoot> evaluate(expression: PredicateExpression<TRoot>, input: TRoot): Boolean {
        return InMemoryCompiler().compile(expression).result.execute(input) == true
    }

    @Test
    fun `isAnyOf should be true for a contained value`() {
        // Arrange
        val expression = RootExpression<String>().isAnyOf(setOf("workflow-1"))

        // Act
        val actual = evaluate(expression, "workflow-1")

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `isAnyOf should be false for a value that is not contained`() {
        // Arrange
        val expression = RootExpression<String>().isAnyOf(setOf("workflow-1", "workflow-2"))

        // Act
        val actual = evaluate(expression, "workflow-3")

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `isAnyOf should be false for an empty candidate set`() {
        // Arrange
        val expression = RootExpression<String>().isAnyOf(emptySet())

        // Act
        val actual = evaluate(expression, "workflow-1")

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `isAnyOf should treat collection and vararg overloads consistently`() {
        // Arrange
        val fromCollection = RootExpression<String>().isAnyOf(listOf("a", "b"))
        val fromVararg = RootExpression<String>().isAnyOf("a", "b")

        // Act & Assert
        listOf("a", "b", "c").forEach { input ->
            assertThat(evaluate(fromCollection, input)).isEqualTo(evaluate(fromVararg, input))
        }
        assertThat(evaluate(fromVararg, "b")).isTrue()
    }

    @Test
    fun `isAnyOf should support nullable values`() {
        // Arrange
        val withNull = RootExpression<String?>().isAnyOf(null, "a")
        val withoutNull = RootExpression<String?>().isAnyOf("a")

        // Act & Assert
        assertThat(evaluate(withNull, null)).isTrue()
        assertThat(evaluate(withNull, "a")).isTrue()
        assertThat(evaluate(withoutNull, null)).isFalse()
        assertThat(evaluate(withoutNull, "b")).isFalse()
    }

    @Test
    fun `negated isAnyOf should invert the result`() {
        // Arrange
        val expression = RootExpression<TestModel>().get(TestModel::name).isAnyOf("a", "b").not()

        // Act & Assert
        assertThat(evaluate(expression, TestModel("a"))).isFalse()
        assertThat(evaluate(expression, TestModel("c"))).isTrue()
    }

    @Test
    fun `isAnyOf should evaluate non-constant candidates against the current input`() {
        // Arrange
        val root = RootExpression<TestModel>()
        val expression = IsAnyOfOperator(
            root.get(TestModel::name),
            setOf(root.get(TestModel::alias))
        )

        // Act & Assert
        assertThat(evaluate(expression, TestModel("a", alias = "a"))).isTrue()
        assertThat(evaluate(expression, TestModel("a", alias = "b"))).isFalse()
    }
}
