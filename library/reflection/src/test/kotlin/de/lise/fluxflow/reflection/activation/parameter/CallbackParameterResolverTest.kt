package de.lise.fluxflow.reflection.activation.parameter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class CallbackParameterResolverTest {

    private val testParameter = FunctionParameter(
                TestClass::testFunction,
                TestClass::testFunction.parameters.last()
    )

    @Test
    fun `resolveParameter should return null if matcher returns false`() {
        // Arrange
        val matcher = mock<ParamMatcher> {
            on { matches(any()) }.doReturn(false)
        }
        val provider = mock<ParameterProvider<Any>> {}
        val callbackParameterResolver = CallbackParameterResolver(
            matcher,
            provider
        )

        // Act
        val result = callbackParameterResolver.resolveParameter(testParameter)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `resolveParameter should return a result if the matcher returns true`() {
        // Arrange
        val matcher = mock<ParamMatcher> {
            on { matches( any()) }.doReturn(true)
        }
        val provider = mock<ParameterProvider<Any>> {}
        val callbackParameterResolver = CallbackParameterResolver(
            matcher,
            provider
        )

        // Act
        val result = callbackParameterResolver.resolveParameter(testParameter)

        // Assert
        assertThat(result).isNotNull
    }

    @Test
    fun `resolveParameter should return a resolution that will lazily invoke the supplied provider`() {
        // Arrange
        val matcher = mock<ParamMatcher> {
            on { matches(any()) }.doReturn(true)
        }
        val expectedValue = Any()
        val provider = mock<ParameterProvider<Any>> {
            on { provide() }.doReturn(expectedValue)
        }
        val callbackParameterResolver = CallbackParameterResolver(
            matcher,
            provider
        )

        // Act
        val result = callbackParameterResolver.resolveParameter(testParameter)

        // Assert
        assertThat(result).isNotNull
        verify(provider, never()).provide()
        val actualValue = result!!.get()
        assertThat(actualValue).isSameAs(expectedValue)
        verify(provider, times(1)).provide()
    }

    @Suppress("UNUSED_PARAMETER")
    private class TestClass {
        fun testFunction(someFunction: Any) {}
    }
}