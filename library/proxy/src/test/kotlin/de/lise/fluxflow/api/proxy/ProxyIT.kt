package de.lise.fluxflow.api.proxy

import de.lise.fluxflow.api.proxy.step.data.DataProxyFactoryImpl
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.data.Data
import de.lise.fluxflow.api.step.stateful.data.StepDataService
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import de.lise.fluxflow.stereotyped.continuation.ContinuationBuilder
import de.lise.fluxflow.stereotyped.metadata.MetadataBuilder
import de.lise.fluxflow.stereotyped.step.StepDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.action.ActionDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.action.ActionFunctionResolverImpl
import de.lise.fluxflow.stereotyped.step.automation.AutomationDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.data.DataDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.data.DataKindInspector
import de.lise.fluxflow.stereotyped.step.data.DataListenerDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.data.validation.ValidationBuilder
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class ProxyIT {
    private val continuationBuilder = ContinuationBuilder()
    private val metadataBuilder = MetadataBuilder(HashMap(), HashMap())
    private val parameterResolver = ParameterResolver { null }
    private val actionFunctionResolver = ActionFunctionResolverImpl(
        parameterResolver
    )
    private val actionDefinitionBuilder = ActionDefinitionBuilder(
        continuationBuilder,
        metadataBuilder,
        actionFunctionResolver,
    )
    private val automationDefinitionBuilder = AutomationDefinitionBuilder(
        continuationBuilder,
        parameterResolver
    )
    private val dataValidationBuilder = mock<ValidationBuilder> {}
    private val dataDefinitionBuilder = DataDefinitionBuilder(
        DataListenerDefinitionBuilder(
            continuationBuilder,
            parameterResolver
        ),
        dataValidationBuilder
    )

    private val stepDefinitionBuilder = StepDefinitionBuilder(
        actionDefinitionBuilder,
        dataDefinitionBuilder,
        metadataBuilder,
        automationDefinitionBuilder,
        HashMap(),
    )


    interface TestStepWithSimpleProp { var name: String }
    class TestStepWithSimplePropImpl(override var name: String) : TestStepWithSimpleProp

    @Test
    fun `proxies should use the StepDataService when reading properties`() {
        // Arrange
        val step = mock<Step> {}

        val testValueFromDataService = "data-service-value"
        val data = mock<Data<String>> {
            on { get() } doReturn testValueFromDataService
        }
        val dataKind = DataKindInspector.getDataKind(TestStepWithSimpleProp::name)
        val stepDataService = mock<StepDataService> {
            on { it.getData<String>(eq(step), eq(dataKind)) } doReturn data
        }

        val dataProxyFactory = DataProxyFactoryImpl(stepDataService)
        val stepDefinition = stepDefinitionBuilder.build(TestStepWithSimplePropImpl("value-from-instance"))
        val proxyTypeFactoryImpl = ProxyTypeFactoryImpl(dataProxyFactory)

        // Act
        val proxyObject = proxyTypeFactoryImpl.createProxy<TestStepWithSimpleProp>(stepDefinition).instantiateProxy(step)
        val returnedName = proxyObject.name

        // Assert
        assertThat(returnedName).isSameAs(testValueFromDataService)
        verify(stepDataService, times(1)).getData<String>(step, dataKind)
    }


    @Test
    fun `proxies should use the StepDataService when assigning properties`() {
        // Arrange
        val step = mock<Step> {}

        val testValueFromDataService = "data-service-value"
        val data = mock<Data<String>> {
            on { get() } doReturn testValueFromDataService
        }
        val dataKind = DataKindInspector.getDataKind(TestStepWithSimpleProp::name)
        val stepDataService = mock<StepDataService> {
            on { getData<String>(eq(step), eq(dataKind)) } doReturn data
        }

        val dataProxyFactory = DataProxyFactoryImpl(stepDataService)
        val stepDefinition = stepDefinitionBuilder.build(TestStepWithSimplePropImpl("value-from-instance"))
        val proxyTypeFactoryImpl = ProxyTypeFactoryImpl(dataProxyFactory)

        val valueToBeAssigned = "new-value"

        // Act
        val proxyObject = proxyTypeFactoryImpl.createProxy<TestStepWithSimpleProp>(stepDefinition).instantiateProxy(step)
        proxyObject.name = valueToBeAssigned

        // Assert
        verify(stepDataService, times(1)).setValue(data, valueToBeAssigned)
    }
}