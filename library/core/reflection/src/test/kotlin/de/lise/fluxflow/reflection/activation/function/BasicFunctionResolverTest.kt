package de.lise.fluxflow.reflection.activation.function

import de.lise.fluxflow.reflection.activation.parameter.FunctionParameter
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolution
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

class BasicFunctionResolverTest {
    private val testConstructor = TestClass::class.constructors.first()

    private val stringParam = FunctionParameter(
        testConstructor,
        testConstructor.parameters[0]
    )
    private val intParam = FunctionParameter(
        testConstructor,
        testConstructor.parameters[1]
    )

    private val defaultConstructor = TestClassWithDefaultConstructor::class.constructors.first()

    @Test
    fun `resolve should return null if at least one parameter could not be resolved`() {
        // Arrange
        val paramResolution = mock<ParameterResolution> {}
        val parameterResolver = mock<ParameterResolver> {
            on { resolveParameter(eq(stringParam)) }.doReturn(paramResolution)
            on { resolveParameter(eq(intParam)) }.doReturn(null)
        }
        val constructorResolver = BasicFunctionResolver(parameterResolver)

        // Act
        val resolution = constructorResolver.resolve(testConstructor)

        // Assert
        assertThat(resolution).isNull()
    }

    @Test
    fun `resolve should return non null if all parameters could be resolved`() {
        // Arrange
        val paramResolution = mock<ParameterResolution> {}
        val parameterResolver = mock<ParameterResolver> {
            on { resolveParameter(eq(stringParam)) }.doReturn(paramResolution)
            on { resolveParameter(eq(intParam)) }.doReturn(paramResolution)
        }
        val constructorResolver = BasicFunctionResolver(parameterResolver)

        // Act
        val resolution = constructorResolver.resolve(testConstructor)

        // Assert
        assertThat(resolution).isNotNull
    }

    @Test
    fun `resolve should return a resolution for default constructors`() {
        // Arrange
        val parameterResolver = mock<ParameterResolver> { on { resolveParameter(any()) }.doReturn(null) }
        val constructorResolver = BasicFunctionResolver(parameterResolver)

        // Act
        val resolution = constructorResolver.resolve(defaultConstructor)

        // Assert
        assertThat(resolution).isNotNull
    }

    private class TestClass(
        private val stringParam: String,
        private val intParam: String
    )
    private class TestClassWithDefaultConstructor()
}