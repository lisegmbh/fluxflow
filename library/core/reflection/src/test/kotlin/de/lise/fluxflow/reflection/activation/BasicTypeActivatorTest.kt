package de.lise.fluxflow.reflection.activation

import de.lise.fluxflow.reflection.activation.function.FunctionResolution
import de.lise.fluxflow.reflection.activation.function.FunctionResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class BasicTypeActivatorTest {
    private val stringIntConstructor = TestClass::class.constructors.first{ it.parameters.size == 2 }
    private val stringConstructor = TestClass::class.constructors.first { it.parameters.size == 1 && it.parameters[0].name == "stringParam" }
    private val intConstructor = TestClass::class.constructors.first { it.parameters.size == 1 && it.parameters[0].name == "intParam" }

    @Test
    fun `findActivation should return null if no suitable constructor could be found`() {
        // Arrange
        val functionResolver = mock<FunctionResolver> { on { resolve<TestClass>(any()) }.doReturn(null) }
        val typeActivator = BasicTypeActivator(functionResolver)

        // Act
        val activation = typeActivator.findActivation(TestClass::class)

        // Assert
        assertThat(activation).isNull()
        verify(functionResolver, times(1)).resolve(stringIntConstructor)
        verify(functionResolver, times(1)).resolve(stringConstructor)
        verify(functionResolver, times(1)).resolve(intConstructor)
    }

    @Test
    fun `findActivation should return an activation based on the first suitable constructor`() {
        // Arrange
        val functionResolution = mock<FunctionResolution<TestClass>> {}
        val functionResolver = mock<FunctionResolver> {
            on { resolve(stringConstructor) }.doReturn(null)
            on { resolve(intConstructor) }.doReturn(null)
            on { resolve(stringIntConstructor) }.doReturn(functionResolution)
        }
        val typeActivator = BasicTypeActivator(functionResolver)

        // Act
        val activation = typeActivator.findActivation(TestClass::class)

        // Assert
        assertThat(activation).isNotNull
        verify(functionResolver, times(1)).resolve(stringIntConstructor)
        verify(functionResolver, times(1)).resolve(stringConstructor)
        verify(functionResolver, times(1)).resolve(intConstructor)
    }

    @Suppress("UNUSED_PARAMETER")
    private class TestClass{
        constructor(stringParam: String, intParam: Int) {}
        constructor(stringParam: String) {}
        constructor(intParam: Int) {}
    }
}