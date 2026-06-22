package de.fluxflow.flowquery.mapper.expression

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.RootExpression
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class ExpressionWalkerTest {
    @Test
    fun `walker should apply and return the new replacement expression`() {
        val walker = ExpressionWalker()
        val originalExpression = Expression.root<NestedElement>()
            .get(NestedElement::value)

        val result = walker.walk(
            ExpressionNode.root(originalExpression)
        ){
            when(it.current.expression) {
                is RootExpression<*> -> {
                    ExpressionWalkerResult.Replace(
                        Expression.root<TestRoot>()
                            .get(TestRoot::nestedElement)
                    )
                }
                else -> ExpressionWalkerResult.Continue
            }
        }

        Assertions.assertThat(result.replaceWith).isEqualTo(
            Expression.root<TestRoot>()
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