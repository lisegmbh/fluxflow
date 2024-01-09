package de.lise.fluxflow.reflection.activation

import de.lise.fluxflow.reflection.activation.function.FunctionResolution
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class BasicTypeActivationTest {
    @Test
    fun `activate should invoke the given constructor and return it's result`() {
        // Arrange
        val constructedObject = Any()
        val functionResolution = mock<FunctionResolution<Any>> {
            on { call() }.doReturn(constructedObject)
        }
        val typeActivation = BasicTypeActivation(functionResolution)

        // Act
        val activatedObject = typeActivation.activate()

        // Assert
        verify(functionResolution, times(1)).call()
        assertThat(activatedObject).isSameAs(constructedObject)
    }
}