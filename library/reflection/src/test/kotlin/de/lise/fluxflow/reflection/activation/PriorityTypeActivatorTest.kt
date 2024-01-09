package de.lise.fluxflow.reflection.activation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class PriorityTypeActivatorTest {
    @Test
    fun `findActivation should return null if no given activator is able to find an activation`() {
        // Arrange
        val activators = listOf(
            mock<TypeActivator> { on { findActivation<TestClass>(any()) }.doReturn(null) },
            mock<TypeActivator> { on { findActivation<TestClass>(any()) }.doReturn(null) },
        )
        val activator = PriorityTypeActivator(activators)

        // Act
        val result = activator.findActivation(TestClass::class)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `findActivation should return the activation returned by the first activator`() {
        // Arrange
        val activator0 = mock<TypeActivator> { on { findActivation<TestClass>(any()) }.doReturn(null) }

        val activation1 = mock<TypeActivation<TestClass>> {}
        val activator1 = mock<TypeActivator> { on { findActivation<TestClass>(any()) }.doReturn(activation1) }

        val activation2 = mock<TypeActivation<TestClass>> {}
        val activator2 = mock<TypeActivator> { on { findActivation<TestClass>(any()) }.doReturn(activation2) }

        val activator = PriorityTypeActivator(listOf(
            activator0,
            activator1,
            activator2
        ))

        // Act
        val result = activator.findActivation(TestClass::class)

        // Assert
        assertThat(result).isSameAs(activation1)
        verify(activator0, times(1)).findActivation(TestClass::class)
        verify(activator1, times(1)).findActivation(TestClass::class)
        verify(activator2, never()).findActivation(TestClass::class)
    }

    private class TestClass
}