package de.lise.fluxflow.reflection.activation.parameter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FunctionParameterTest {
    @Test
    fun `matchingProperty should return KProperty created for default constructor`() {
        // Arrange
        val constructor = SomeDataClass::class.constructors.single()
        val parameter = constructor.parameters.single { 
            it.name == SomeDataClass::someProperty.name 
        }
        val functionParameter = FunctionParameter(constructor, parameter)
    
        // Act
        val result = functionParameter.matchingProperty
        
        // Assert
        assertThat(result).isEqualTo(SomeDataClass::someProperty)
    }

    @Test
    fun `matchingProperty should return manually created properties`() {
        // Arrange
        val constructor = SomeClass::class.constructors.single()
        val parameter = constructor.parameters.single {
            it.name == SomeClass::someProperty.name
        }
        val functionParameter = FunctionParameter(constructor, parameter)

        // Act
        val result = functionParameter.matchingProperty

        // Assert
        assertThat(result).isEqualTo(SomeClass::someProperty)
    }
    
    data class SomeDataClass(val someProperty: Any?)
    
    class SomeClass {
        val someProperty: String
        
        @Suppress("unused")
        constructor(someProperty: String) {
            this.someProperty = someProperty
        }
    }
}