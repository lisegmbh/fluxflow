package de.lise.fluxflow.reflection.activation.parameter

import de.lise.fluxflow.api.ioc.IocProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import kotlin.reflect.KType

class IocParameterResolverTest {
    @Test
    fun `resolve should return values from IoC, if available`() {
        // Arrange
        val iocProvider = mock<IocProvider> {
            on { provide(any<KType>()) }.thenReturn("some string value")
        }
        val param = FunctionParameter(
            TestClass::class.constructors.first(),
            TestClass::class.constructors.first().parameters.first()
        )
        val resolver = IocParameterResolver(
            iocProvider
        )

        // Act
        val resolution = resolver.resolveParameter(param)

        // Assert
        assertThat(resolution).isNotNull
        assertThat(resolution!!.get()).isEqualTo("some string value")
    }

    @Test
    fun `resolve should return null, if IoC does not know how to resolve the requested type`() {
        // Arrange
        val iocProvider = mock<IocProvider> {
            on { provide(any<KType>()) }.thenReturn(null)
        }
        val param = FunctionParameter(
            TestClass::class.constructors.first(),
            TestClass::class.constructors.first().parameters.first()
        )
        val resolver = IocParameterResolver(
            iocProvider
        )

        // Act
        val resolution = resolver.resolveParameter(param)

        // Assert
        assertThat(resolution).isNull()
    }

    class TestClass(
        private val someStringArgs: String
    )
}