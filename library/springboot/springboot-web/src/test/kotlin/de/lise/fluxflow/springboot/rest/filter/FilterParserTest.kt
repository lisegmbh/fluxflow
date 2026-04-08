package de.lise.fluxflow.springboot.rest.filter

import de.fluxflow.flowquery.expression.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FilterParserTest {

    private val parser = FilterParser(PersonModel::class)

    // -- eq --

    @Test
    fun `parses eq operator`() {
        val result = parser.parse("age eq 30")

        assertThat(result).isInstanceOf(BinaryOperationExpression::class.java)
        assertThat((result as BinaryOperationExpression<*, *, *>).operation).isEqualTo(BinaryOperation.Equal)
    }

    // -- ne --

    @Test
    fun `parses ne operator`() {
        val result = parser.parse("age ne 30")

        assertThat(result).isInstanceOf(BinaryOperationExpression::class.java)
        assertThat((result as BinaryOperationExpression<*, *, *>).operation).isEqualTo(BinaryOperation.NotEqual)
    }

    // -- gt --

    @Test
    fun `parses gt operator`() {
        val result = parser.parse("age gt 18")

        assertThat(result).isInstanceOf(BinaryOperationExpression::class.java)
        assertThat((result as BinaryOperationExpression<*, *, *>).operation).isEqualTo(BinaryOperation.GreaterThan)
    }

    // -- lt --

    @Test
    fun `parses lt operator`() {
        val result = parser.parse("age lt 50")

        assertThat(result).isInstanceOf(BinaryOperationExpression::class.java)
        assertThat((result as BinaryOperationExpression<*, *, *>).operation).isEqualTo(BinaryOperation.LessThan)
    }

    // -- ge --

    @Test
    fun `parses ge operator`() {
        val result = parser.parse("age ge 18")

        assertThat(result).isInstanceOf(BinaryOperationExpression::class.java)
        assertThat((result as BinaryOperationExpression<*, *, *>).operation).isEqualTo(BinaryOperation.GreaterThanOrEqual)
    }

    // -- le --

    @Test
    fun `parses le operator`() {
        val result = parser.parse("age le 50")

        assertThat(result).isInstanceOf(BinaryOperationExpression::class.java)
        assertThat((result as BinaryOperationExpression<*, *, *>).operation).isEqualTo(BinaryOperation.LessThanOrEqual)
    }

    // -- and --

    @Test
    fun `parses and expression`() {
        val result = parser.parse("age gt 18 and isActive")

        assertThat(result).isInstanceOf(AndExpression::class.java)
        assertThat((result as AndExpression<*>).predicates).hasSize(2)
    }

    // -- or --

    @Test
    fun `parses or expression`() {
        val result = parser.parse("age lt 10 or age gt 50")

        assertThat(result).isInstanceOf(OrExpression::class.java)
        assertThat((result as OrExpression<*>).predicates).hasSize(2)
    }

    // -- not --

    @Test
    fun `parses not expression`() {
        val result = parser.parse("not isActive")

        assertThat(result).isInstanceOf(NotExpression::class.java)
    }

    // -- in --

    @Test
    fun `parses in expression`() {
        val result = parser.parse("age in (10, 20, 30)")

        assertThat(result).isInstanceOf(IsAnyOfOperator::class.java)
        assertThat((result as IsAnyOfOperator<*, *>).anyOf).hasSize(3)
    }

    // -- contains --

    @Test
    fun `parses contains function`() {
        val result = parser.parse("contains(name, 'x')")

        assertThat(result).isInstanceOf(ContainsExpression::class.java)
    }

    // -- literals --

    @Test
    fun `parses integer literal`() {
        val result = parser.parse("age eq 42") as BinaryOperationExpression<*, *, *>

        assertThat(result.rightOperand).isInstanceOf(ConstantExpression::class.java)
        assertThat((result.rightOperand as ConstantExpression<*, *>).value).isEqualTo(42)
    }

    @Test
    fun `parses double literal`() {
        val result = parser.parse("score eq 3.14") as BinaryOperationExpression<*, *, *>

        assertThat(result.rightOperand).isInstanceOf(ConstantExpression::class.java)
        assertThat((result.rightOperand as ConstantExpression<*, *>).value).isEqualTo(3.14)
    }

    @Test
    fun `parses string literal`() {
        val result = parser.parse("name eq 'hello'") as BinaryOperationExpression<*, *, *>

        assertThat(result.rightOperand).isInstanceOf(ConstantExpression::class.java)
        assertThat((result.rightOperand as ConstantExpression<*, *>).value).isEqualTo("hello")
    }

    @Test
    fun `parses boolean literal`() {
        val result = parser.parse("isActive eq true") as BinaryOperationExpression<*, *, *>

        assertThat(result.rightOperand).isInstanceOf(ConstantExpression::class.java)
        assertThat((result.rightOperand as ConstantExpression<*, *>).value).isEqualTo(true)
    }

    @Test
    fun `parses null literal`() {
        val result = parser.parse("name eq null") as BinaryOperationExpression<*, *, *>

        assertThat(result.rightOperand).isInstanceOf(ConstantExpression::class.java)
        assertThat((result.rightOperand as ConstantExpression<*, *>).value).isNull()
    }

    @Test
    fun `unescapes single quote in string literal`() {
        val result = parser.parse("name eq 'it''s'") as BinaryOperationExpression<*, *, *>

        assertThat((result.rightOperand as ConstantExpression<*, *>).value).isEqualTo("it's")
    }

    // -- error cases --

    @Test
    fun `throws on unknown property`() {
        assertThrows<ExpressionParsingException> {
            parser.parse("unknown eq 'x'")
        }
    }

    @Test
    fun `throws on syntax error`() {
        assertThrows<ExpressionParsingException> {
            parser.parse("age !! 5")
        }
    }

    interface PersonModel {
        val age: Int
        val score: Double
        val name: String?
        val isActive: Boolean
    }
}
