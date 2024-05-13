package de.lise.fluxflow.reflection.activation.parameter

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaType

class ParamMatcherTest {
    private val someGenericProp: List<String>? = null
    private fun testFunWithMatchingParam(foo: List<String>?) {}
    private fun testFunWithNonMatchingParam(foo: List<Int>) {}
    
    @Test
    fun `isAssignableFrom should return a matcher that supports java's generic types`(){
        // Arrange
        val someGenericType = ParamMatcherTest::someGenericProp.returnType.javaType
        
        val matchingFunction: KFunction<*> = ParamMatcherTest::testFunWithMatchingParam
        val nonMatchingFunction: KFunction<*> = ParamMatcherTest::testFunWithNonMatchingParam
        
        // Act
        val paramMatcher = ParamMatcher.isAssignableFrom(someGenericType)
        
        // Assert
        val positiveResult = paramMatcher.matches(
            FunctionParameter(
                matchingFunction,
                matchingFunction.parameters[1]
            )
        )
        assertThat(positiveResult).isTrue
        
        val negativeResult = paramMatcher.matches(
            FunctionParameter(
                nonMatchingFunction,
                nonMatchingFunction.parameters[1]
            )
        )
        assertThat(negativeResult).isFalse
    }

    @Test
    fun `isAssignableFrom should return a matcher that supports kotlin's generic types`(){
        // Arrange
        val someGenericType = ParamMatcherTest::someGenericProp.returnType

        val matchingFunction: KFunction<*> = ParamMatcherTest::testFunWithMatchingParam
        val nonMatchingFunction: KFunction<*> = ParamMatcherTest::testFunWithNonMatchingParam

        // Act
        val paramMatcher = ParamMatcher.isAssignableFrom(someGenericType)

        // Assert
        val positiveResult = paramMatcher.matches(
            FunctionParameter(
                matchingFunction,
                matchingFunction.parameters[1]
            )
        )
        assertThat(positiveResult).isTrue

        val negativeResult = paramMatcher.matches(
            FunctionParameter(
                nonMatchingFunction,
                nonMatchingFunction.parameters[1]
            )
        )
        assertThat(negativeResult).isFalse
    }
}