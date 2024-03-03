package de.lise.fluxflow.reflection.activation.function

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class PriorityFunctionResolverTest {
    private val testConstructor = TestClass::class.constructors.first()

    @Test
    fun `resolve should return null if no given resolver is able to resolve the constructor`(){
        // Arrange
        val resolvers = listOf(
            mock<FunctionResolver> { on { resolve<Any>(any()) }.thenReturn(null) },
            mock<FunctionResolver> { on { resolve<Any>(any()) }.thenReturn(null) },
        )
        val resolver = PriorityFunctionResolver(resolvers)

        // Act
        val resolution = resolver.resolve(testConstructor)

        // Assert
        assertThat(resolution).isNull()
    }

    @Test
    fun `resolve should return the resolution returned by the first resolver that is able to resolve the given constructor`() {
        // Arrange
        val resolver0 = mock<FunctionResolver> { on { resolve<Any>(any()) }.thenReturn(null) }

        val resolution1 = mock<FunctionResolution<Any>> {}
        val resolver1 = mock<FunctionResolver> { on { resolve<Any>(any()) }.thenReturn(resolution1)}

        val resolution2 = mock<FunctionResolution<Any>> {}
        val resolver2 = mock<FunctionResolver> { on { resolve<Any>(any()) }.thenReturn(resolution2)}

        val resolver = PriorityFunctionResolver(listOf(
            resolver0,
            resolver1,
            resolver2
        ))

        // Act
        val resolution = resolver.resolve(testConstructor)

        // Assert
        assertThat(resolution).isSameAs(resolution1)

        verify(resolver0, times(1)).resolve<Any>(any())
        verify(resolver1, times(1)).resolve<Any>(any())
        verify(resolver2, never()).resolve<Any>(any())
    }

    private class TestClass(
        private val testParameter: Any
    )
}