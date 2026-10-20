package de.lise.fluxflow.reflection

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf

class FunctionExtensionFunctionsTest {
    @Test
    fun `declaringType should return the declaring type for constructor functions`() {
        // Act
        val result = TestClass::class.constructors.single().declaringType
        
        // Assert
        assertThat(result).isEqualTo(typeOf<TestClass>())
    }

    @Test
    fun `declaringType should return the declaring type for member functions`() {
        // Act
        val result = TestClass::doSomething.declaringType

        // Assert
        assertThat(result).isEqualTo(typeOf<TestClass>())
    }
    
    @Suppress("unused")
    class TestClass {
        constructor(someValue: Any)
        fun doSomething() {}
    }
}