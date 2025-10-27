package de.fluxflow.flowquery.mapper.expression

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.RootExpression
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class ExpressionWalkerTest {
    @Test
    fun `walker should apply and return the new replacement expression`() {
        val walker = ExpressionWalker()
        val originalExpression = Expression.Companion.root<NestedElement>()
            .get(NestedElement::value)

        val result = walker.walk(originalExpression) {
            when(it) {
                is RootExpression<*> -> {
                    ExpressionWalkerResult.Replace(
                        Expression.Companion.root<TestRoot>()
                            .get(TestRoot::nestedElement)
                    )
                }
                else -> ExpressionWalkerResult.Continue
            }
        }

        Assertions.assertThat(result.replaceWith).isEqualTo(
            Expression.Companion.root<TestRoot>()
                .get(TestRoot::nestedElement)
                .get(NestedElement::value)
        )

        Assertions.assertThat(result).isNotNull()
    }


    data class NestedElement(
        val value: String
    )

    data class TestRoot(
        val nestedElement: NestedElement
    )
}