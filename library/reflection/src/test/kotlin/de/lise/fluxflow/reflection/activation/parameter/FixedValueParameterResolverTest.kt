package de.lise.fluxflow.reflection.activation.parameter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class FixedValueParameterResolverTest {

    private val testParameter = FunctionParameter(
        TestClass::class.constructors.first(),
        TestClass::class.constructors.first().parameters.first()
    )

    @Test
    fun `resolveParameter should return null, if the matcher did not match`() {
        // Arrange
        val matcher = mock<ValueMatcher<Any>> {
            on { matches(any(), any()) }.doReturn(false)
        }
        val resolver = FixedValueParameterResolver(matcher, Any())

        // Act
        val resolution = resolver.resolveParameter(testParameter)

        // Assert
        assertThat(resolution).isNull()
    }

    @Test
    fun `resolveParameter should return the given value, if the matcher matches`() {
        // Arrange
        val testValue = Any()
        val matcher = mock<ValueMatcher<Any>> {
            on { matches(any(), any()) }.doReturn(true)
        }
        val resolver = FixedValueParameterResolver(matcher, testValue)

        // Act
        val resolution = resolver.resolveParameter(testParameter)
        val returnedValue = resolution?.get()

        // Assert
        assertThat(resolution).isNotNull
        assertThat(returnedValue).isSameAs(testValue)
    }

    private class TestClass(
        private val testParameter: Any
    )

}