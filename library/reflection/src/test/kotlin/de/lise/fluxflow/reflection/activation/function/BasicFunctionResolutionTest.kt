package de.lise.fluxflow.reflection.activation.function

import de.lise.fluxflow.reflection.activation.parameter.ParameterResolution
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class BasicFunctionResolutionTest {
    private val testConstructor = TestClass::class.constructors.first()

    @Test
    fun `construct should use all provided parameter resolutions`() {
        // Arrange
        val paramResolution1 = mock<ParameterResolution> { on { get() }.doReturn("Hello World") }
        val paramResolution2 = mock<ParameterResolution> { on { get() }.doReturn(42) }
        val resolution = BasicFunctionResolution(
            testConstructor,
            listOf(
                paramResolution1,
                paramResolution2
            )
        )

        // Act
        resolution.call()

        // Assert
        verify(paramResolution1, times(1)).get()
        verify(paramResolution2, times(1)).get()
    }

    @Test
    fun `construct should invoke the given constructor using the resolved parameters`() {
        // Arrange
        val paramResolution1 = mock<ParameterResolution> { on { get() }.doReturn("Hello World") }
        val paramResolution2 = mock<ParameterResolution> { on { get() }.doReturn(42) }
        val resolution = BasicFunctionResolution(
            testConstructor,
            listOf(
                paramResolution1,
                paramResolution2
            )
        )

        // Act
        val result = resolution.call()

        // Assert
        assertThat(result).isInstanceOf(TestClass::class.java)
        assertThat(result.stringParam).isEqualTo("Hello World")
        assertThat(result.intParam).isEqualTo(42)
    }

    class TestClass(
         val stringParam: String,
         val intParam: Int
    )
}