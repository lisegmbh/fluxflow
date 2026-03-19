package de.lise.fluxflow.springboot.rest.filter

import de.fluxflow.flowquery.inmemory.expression.compilation.InMemoryCompiler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class FilterParserIT {

    private val parser = FilterParser(PersonModel::class)
    private val compiler = InMemoryCompiler()

    private val testPerson = mock<PersonModel> {
        on { age } doReturn 30
        on { score } doReturn 2.5
        on { name } doReturn "Alice"
        on { isActive } doReturn true
    }

    private fun evaluate(filter: String): Boolean =
        @Suppress("UNCHECKED_CAST")
        (compiler.compile(parser.parse(filter)).result.execute(testPerson) as Boolean)

    // -- eq --

    @Test
    fun `age eq matches`() {
        assertThat(evaluate("age eq 30")).isTrue()
    }

    @Test
    fun `age eq does not match`() {
        assertThat(evaluate("age eq 99")).isFalse()
    }

    // -- ne --

    @Test
    fun `age ne matches`() {
        assertThat(evaluate("age ne 99")).isTrue()
    }

    @Test
    fun `age ne does not match`() {
        assertThat(evaluate("age ne 30")).isFalse()
    }

    // -- gt --

    @Test
    fun `age gt matches`() {
        assertThat(evaluate("age gt 18")).isTrue()
    }

    @Test
    fun `age gt does not match`() {
        assertThat(evaluate("age gt 30")).isFalse()
    }

    // -- ge --

    @Test
    fun `age ge matches exact value`() {
        assertThat(evaluate("age ge 30")).isTrue()
    }

    @Test
    fun `age ge matches greater value`() {
        assertThat(evaluate("age ge 18")).isTrue()
    }

    // -- lt --

    @Test
    fun `age lt matches`() {
        assertThat(evaluate("age lt 50")).isTrue()
    }

    @Test
    fun `age lt does not match`() {
        assertThat(evaluate("age lt 30")).isFalse()
    }

    // -- le --

    @Test
    fun `age le matches exact value`() {
        assertThat(evaluate("age le 30")).isTrue()
    }

    @Test
    fun `age le matches smaller value`() {
        assertThat(evaluate("age le 50")).isTrue()
    }

    // -- double --

    @Test
    fun `score gt matches double`() {
        assertThat(evaluate("score gt 1.5")).isTrue()
    }

    // -- and --

    @Test
    fun `and both conditions true`() {
        assertThat(evaluate("isActive eq true and age gt 18")).isTrue()
    }

    @Test
    fun `and one condition false`() {
        assertThat(evaluate("isActive eq false and age gt 18")).isFalse()
    }

    // -- or --

    @Test
    fun `or one condition true`() {
        assertThat(evaluate("age lt 10 or age gt 20")).isTrue()
    }

    @Test
    fun `or both conditions false`() {
        assertThat(evaluate("age lt 10 or age gt 50")).isFalse()
    }

    // -- not --

    @Test
    fun `not inverts false`() {
        assertThat(evaluate("not (age gt 50)")).isTrue()
    }

    @Test
    fun `not inverts true`() {
        assertThat(evaluate("not (age gt 18)")).isFalse()
    }

    // -- in --

    @Test
    fun `in matches`() {
        assertThat(evaluate("age in (10, 20, 30)")).isTrue()
    }

    @Test
    fun `in does not match`() {
        assertThat(evaluate("age in (10, 20)")).isFalse()
    }

    @Test
    fun `string in matches`() {
        assertThat(evaluate("name in ('Alice', 'Bob')")).isTrue()
    }

    // -- contains --

    @Test
    fun `contains matches`() {
        assertThat(evaluate("contains(name, 'lic')")).isTrue()
    }

    @Test
    fun `contains does not match`() {
        assertThat(evaluate("contains(name, 'xyz')")).isFalse()
    }

    // -- null --

    @Test
    fun `ne null matches non-null value`() {
        assertThat(evaluate("name ne null")).isTrue()
    }

    // -- grouping --

    @Test
    fun `parentheses grouping`() {
        assertThat(evaluate("not (age gt 18 and isActive)")).isFalse()
    }

    interface PersonModel {
        val age: Int
        val score: Double
        val name: String?
        val isActive: Boolean
    }
}
