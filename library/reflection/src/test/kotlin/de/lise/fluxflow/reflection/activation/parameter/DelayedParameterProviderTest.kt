package de.lise.fluxflow.reflection.activation.parameter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DelayedParameterProviderTest {
    @Test
    fun `provide should throw a null pointer exception if the value has not been set`() {
        // Arrange
        val provider = DelayedParameterProvider<String>()

        // Act & Assert
        assertThrows<NullPointerException> {
            provider.provide()
        }
    }

    @Test
    fun `provider should not throw an exception if the value has been actively set to null`() {
        // Arrange
        val provider = DelayedParameterProvider<String?>()
        provider.set(null)

        // Act
        val result = provider.provide()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `provider should return the value that has been set prior to invocation`() {
        // Arrange
        val provider = DelayedParameterProvider<Any>()
        val expectedValue = Any()
        provider.set(expectedValue)

        // Act
        val actualValue = provider.provide()

        // Assert
        assertThat(actualValue).isSameAs(expectedValue)
    }
}