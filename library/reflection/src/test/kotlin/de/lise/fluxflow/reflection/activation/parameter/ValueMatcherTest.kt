package de.lise.fluxflow.reflection.activation.parameter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ValueMatcherTest {
    private val testParam = FunctionParameter(
        TestClass::class.constructors.first(),
        TestClass::class.constructors.first().parameters.first()
    )
    private val nullableTestParam = FunctionParameter(
        TestClass::class.constructors.first(),
        TestClass::class.constructors.first().parameters[1]
    )
    private val stringTestParam = FunctionParameter(
        TestClass::class.constructors.first(),
        TestClass::class.constructors.first().parameters[2]
    )

    @Test
    fun `and should return a matcher that returns true, if both matchers return true`() {
        // Arrange
        val valueMatcher1 = ValueMatcher<Any> { _, _ -> true }
        val valueMatcher2 = ValueMatcher<Any> { _, _ -> true }
        val matcher = valueMatcher1.and(valueMatcher2)

        // Act
        val result = matcher.matches(testParam, Any())

        // Assert
        assertThat(result).isTrue
    }

    @Test
    fun `and should return a matcher that returns false, if the first matchers returns false`() {
        // Arrange
        val valueMatcher1 = ValueMatcher<Any> { _, _ -> false }
        val valueMatcher2 = ValueMatcher<Any> { _, _ -> true }
        val matcher = valueMatcher1.and(valueMatcher2)

        // Act
        val result = matcher.matches(testParam, Any())

        // Assert
        assertThat(result).isFalse
    }

    @Test
    fun `and should return a matcher that returns false, if the second matchers returns false`() {
        // Arrange
        val valueMatcher1 = ValueMatcher<Any> { _, _ -> true }
        val valueMatcher2 = ValueMatcher<Any> { _, _ -> false }
        val matcher = valueMatcher1.and(valueMatcher2)

        // Act
        val result = matcher.matches(testParam, Any())

        // Assert
        assertThat(result).isFalse
    }

    @Test
    fun `and should return a matcher that returns false, if both matchers return false`() {
        // Arrange
        val valueMatcher1 = ValueMatcher<Any> { _, _ -> true }
        val valueMatcher2 = ValueMatcher<Any> { _, _ -> false }
        val matcher = valueMatcher1.and(valueMatcher2)

        // Act
        val result = matcher.matches(testParam, Any())

        // Assert
        assertThat(result).isFalse
    }


    @Test
    fun `or should return a matcher that returns true, if both matchers return true`() {
        // Arrange
        val valueMatcher1 = ValueMatcher<Any> { _, _ -> true }
        val valueMatcher2 = ValueMatcher<Any> { _, _ -> true }
        val matcher = valueMatcher1.or(valueMatcher2)

        // Act
        val result = matcher.matches(testParam, Any())

        // Assert
        assertThat(result).isTrue
    }

    @Test
    fun `or should return a matcher that returns true, if the first matcher returns true`() {
        // Arrange
        val valueMatcher1 = ValueMatcher<Any> { _, _ -> true }
        val valueMatcher2 = ValueMatcher<Any> { _, _ -> false }
        val matcher = valueMatcher1.or(valueMatcher2)

        // Act
        val result = matcher.matches(testParam, Any())

        // Assert
        assertThat(result).isTrue
    }

    @Test
    fun `or should return a matcher that returns true, if the second matcher returns true`() {
        // Arrange
        val valueMatcher1 = ValueMatcher<Any> { _, _ -> false }
        val valueMatcher2 = ValueMatcher<Any> { _, _ -> true }
        val matcher = valueMatcher1.or(valueMatcher2)

        // Act
        val result = matcher.matches(testParam, Any())

        // Assert
        assertThat(result).isTrue
    }

    @Test
    fun `or should return a matcher that returns false, if both matchers returns false`() {
        // Arrange
        val valueMatcher1 = ValueMatcher<Any> { _, _ -> false }
        val valueMatcher2 = ValueMatcher<Any> { _, _ -> false }
        val matcher = valueMatcher1.or(valueMatcher2)

        // Act
        val result = matcher.matches(testParam, Any())

        // Assert
        assertThat(result).isFalse
    }

    @Test
    fun `hasName should return true, if the given name matches the formal parameter's name`() {
        // Arrange
        val matcher = ValueMatcher.hasName<Any>("testParam")

        // Act
        val result = matcher.matches(testParam, "a value")

        // Assert
        assertThat(result).isTrue
    }

    @Test
    fun `hasName should return false, if the given name dosen't match the formal parameter's name`() {
        // Arrange
        val matcher = ValueMatcher.hasName<Any>("TestParam")

        // Act
        val result = matcher.matches(testParam, "a value")

        // Assert
        assertThat(result).isFalse
    }

    @Test
    fun `valueIsNotNull should return true, if the given value is not null`() {
        // Arrange
        val matcher = ValueMatcher.valueIsNotNull<String?>()

        // Act
        val result = matcher.matches(testParam, "non null value")

        // Assert
        assertThat(result).isTrue
    }

    @Test
    fun `valueIsNotNull should return false, if the given value is null`() {
        // Arrange
        val matcher = ValueMatcher.valueIsNotNull<String?>()

        // Act
        val result = matcher.matches(testParam, null)

        // Assert
        assertThat(result).isFalse
    }

    @Test
    fun `canBeAssigned should return false, if the parameter is not marked nullable and the value is null`() {
        // Arrange
        val matcher = ValueMatcher.canBeAssigned<String?>()

        // Act
        val result = matcher.matches(testParam, null)

        // Assert
        assertThat(result).isFalse
    }

    @Test
    fun `canBeAssigned should return true, if the parameter is marked as nullable and the value is null`() {
        // Arrange
        val matcher = ValueMatcher.canBeAssigned<String?>()

        // Act
        val result = matcher.matches(nullableTestParam, null)

        // Assert
        assertThat(result).isTrue
    }

    @Test
    fun `canBeAssigned should return true, if the value has the same type as the parameter`() {
        // Arrange
        val matcher = ValueMatcher.canBeAssigned<Any>()

        // Act
        val result = matcher.matches(testParam, Any())

        // Assert
        assertThat(result).isTrue
    }

    @Test
    fun `canBeAssigned should return true, if the value is a subtype of the parameter`() {
        // Arrange
        val matcher = ValueMatcher.canBeAssigned<Any>()

        // Act
        val result = matcher.matches(testParam, "a string value")

        // Assert
        assertThat(result).isTrue
    }

    @Test
    fun `canBeAssigned should return false, if the value can not be assigned to the parameter's type`() {
        // Arrange
        val matcher = ValueMatcher.canBeAssigned<Any>()

        // Act
        val result = matcher.matches(stringTestParam, Any())

        // Assert
        assertThat(result).isFalse
    }

    private class TestClass(
        private val testParam: Any,
        private val nullableTestParam: Any?,
        private val stringTestParam: String
    )
}