package de.lise.fluxflow.springboot.activation.parameter

import de.lise.fluxflow.reflection.activation.parameter.FunctionParameter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory

class SpringValueExpressionParameterResolverTest {
    
    @Test
    fun `resolve should not return a resolution if the parameter is not annotated with @Value`() {
        // Arrange
        val factory = mock<ConfigurableBeanFactory> {}
        val functionParamWithoutValue = FunctionParameter(
            SpringValueExpressionParameterResolverTest::functionParamWithoutValue,
            SpringValueExpressionParameterResolverTest::functionParamWithoutValue.parameters.last()
        )
        val resolver = SpringValueExpressionParameterResolver(
            factory
        )
        
        // Act
        val resolution = resolver.resolveParameter(functionParamWithoutValue)
        
        // Assert
        assertThat(resolution).isNull()
    }
    
    @Test
    fun `resolve should return a resolution whenever the parameter is annotated with @Value`() {
        // Arrange
        val factory = mock<ConfigurableBeanFactory> {}
        val functionParamWithValue = FunctionParameter(
            SpringValueExpressionParameterResolverTest::functionParamWithValue,
            SpringValueExpressionParameterResolverTest::functionParamWithValue.parameters.last()
        )
        val resolver = SpringValueExpressionParameterResolver(
            factory
        )

        // Act
        val resolution = resolver.resolveParameter(functionParamWithValue)

        // Assert
        assertThat(resolution).isNotNull()
    }
    
    fun functionParamWithoutValue(someParam: String) {}
    fun functionParamWithValue(@Value("\${some.property}") someParam: String) {}
}