package de.lise.fluxflow.api.proxy

import de.lise.fluxflow.api.proxy.step.StepAccessor
import de.lise.fluxflow.api.proxy.step.data.DataProxyFactory
import de.lise.fluxflow.api.proxy.step.data.DataProxyFactoryImpl
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepDefinition
import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.step.stateful.StatefulStepDefinition
import de.lise.fluxflow.api.step.stateful.data.StepDataService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*

class ProxyTypeFactoryImplTest {
    class SomeFinalClass
    @Test
    fun `createStepProxy should throw an InvalidProxyInterfaceException for final proxy interface types`() {
        // Arrange
        val proxyFactoryImpl = ProxyTypeFactoryImpl(DataProxyFactoryImpl(mock<StepDataService>{}))
        val stepDefinition = mock<StepDefinition> {}

        // Act & Assert
        assertThrows<InvalidProxyInterfaceException> {
            proxyFactoryImpl.createProxy<SomeFinalClass>(stepDefinition)
        }
    }

    open class SomeOpenClass
    @Test
    fun `createProxy should throw an InvalidProxyInterfaceException for class types`() {
        // Arrange
        val proxyFactoryImpl = ProxyTypeFactoryImpl(DataProxyFactoryImpl(mock<StepDataService>{}))
        val stepDefinition = mock<StepDefinition> {}

        // Act & Assert
        assertThrows<InvalidProxyInterfaceException> {
            proxyFactoryImpl.createProxy<SomeOpenClass>(stepDefinition)
        }
    }

    interface SomeEmptyInterface
    @Test
    fun `createProxy should succeed for interface types`() {
        // Arrange
        val dataProxyFactory = mock<DataProxyFactory> { }
        val proxyTypeFactory = ProxyTypeFactoryImpl(
            dataProxyFactory,
        )
        val stepDefinition = mock<StepDefinition> {}

        // Act
        val proxy = proxyTypeFactory.createProxy<SomeEmptyInterface>(stepDefinition)

        // Assert
        assertThat(proxy).isNotNull()
    }

    @Test
    fun `created proxies should refer to the step api object it has been created for`() {
        // Arrange
        val dataProxyFactory = mock<DataProxyFactory> { }
        val proxyTypeFactory = ProxyTypeFactoryImpl(
            dataProxyFactory,
        )
        val stepDefinition = mock<StepDefinition> { }
        val step = mock<Step> { }

        // Act
        val proxy = proxyTypeFactory.createProxy<SomeEmptyInterface>(stepDefinition).instantiateProxy(step)

        // Assert
        assertThat(proxy.let { it as StepAccessor }._proxyStep).isSameAs(step)
    }

    interface InterfaceWithSimpleProp { val name: String }
    @Test
    fun `createProxy should fail if the given interface expects properties but the step is stateless`() {
        // Arrange
        val dataProxyFactory = mock<DataProxyFactory> { }
        val proxyTypeFactory = ProxyTypeFactoryImpl(
            dataProxyFactory,
        )
        val nonStatefulStepDefinition = mock<StepDefinition> {
            on { kind } doReturn StepKind("some-kind")
        }
        val step = mock<Step> { }

        // Act & Assert
        assertThrows<ProxyCreationException> {
            proxyTypeFactory.createProxy<InterfaceWithSimpleProp>(nonStatefulStepDefinition).instantiateProxy(step)
        }
    }

    @Test
    fun `createProxy should use the provided DataProxyFactory whenever the step is stateful`() {
        // Arrange
        val dataProxyFactory = mock<DataProxyFactory> {
            on { it.appendDataProxies<Any>(
                any(), // 0
                any(), // 1
                any(), // 2
            ) } doAnswer { invocationOnMock ->
                invocationOnMock.arguments[2] as ProxyBuilder
            }
        }
        val proxyTypeFactory = ProxyTypeFactoryImpl(
            dataProxyFactory,
        )
        val stepDefinition = mock<StatefulStepDefinition> {}

        // Act
        proxyTypeFactory.createProxy<InterfaceWithSimpleProp>(stepDefinition)

        // Assert
        verify(dataProxyFactory, times(1)).appendDataProxies<InterfaceWithSimpleProp>(
            eq(stepDefinition),
            eq(InterfaceWithSimpleProp::class),
            any()
        )
    }

    @Test
    fun `createProxy should not use the provided DataProxyFactory whenever the step is stateless`() {
        // Arrange
        val dataProxyFactory = mock<DataProxyFactory> { }
        val proxyTypeFactory = ProxyTypeFactoryImpl(
            dataProxyFactory,
        )
        val stepDefinition = mock<StepDefinition> {}

        // Act
        proxyTypeFactory.createProxy<SomeEmptyInterface>(stepDefinition)

        // Assert
        verify(dataProxyFactory, never()).appendDataProxies<InterfaceWithSimpleProp>(
            any(),
            any(),
            any()
        )
    }
}