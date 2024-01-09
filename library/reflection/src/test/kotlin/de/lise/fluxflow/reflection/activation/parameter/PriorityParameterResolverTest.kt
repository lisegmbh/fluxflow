package de.lise.fluxflow.reflection.activation.parameter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class PriorityParameterResolverTest {

    private val testParameter = FunctionParameter(
        TestClass::class.constructors.first(),
        TestClass::class.constructors.first().parameters.first()
    )

    @Test
    fun `resolve should return null if no given provider is able to resolve the parameter`() {
        // Arrange
        val resolvers = listOf<ParameterResolver>(
            mock<ParameterResolver> { on { resolveParameter(any()) }.thenReturn(null) },
            mock<ParameterResolver> { on { resolveParameter(any()) }.thenReturn(null) }
        )
        val resolver = PriorityParameterResolver(resolvers)

        // Act
        val resolution = resolver.resolveParameter(testParameter)

        // Assert
        assertThat(resolution).isNull()
    }

    @Test
    fun `resolve should return the resolution returned by the first provider that is able to resolve the given parameter`() {
        // Arrange
        val resolver0 = mock<ParameterResolver> {
            on { resolveParameter(any<FunctionParameter<*>>()) }.doReturn(null)
        }

        val resolution1 = mock<ParameterResolution> {}
        val resolver1 = mock<ParameterResolver> {
            on { resolveParameter(any<FunctionParameter<*>>()) }.doReturn(resolution1)
        }

        val resolution2 = mock<ParameterResolution> {}
        val resolver2 = mock<ParameterResolver> {
            on { resolveParameter(any<FunctionParameter<*>>()) }.doReturn(resolution2)
        }

        val resolver = PriorityParameterResolver(listOf(
            resolver0,
            resolver1,
            resolver2
        ))

        // Act
        val resolution = resolver.resolveParameter(testParameter)

        // Assert
        assertThat(resolution).isSameAs(resolution1)
        verify(resolver0, times(1)).resolveParameter(any())
        verify(resolver1, times(1)).resolveParameter(any())
        verify(resolver2, never()).resolveParameter(any())
    }

    class TestClass(private val someValue: Any)
}