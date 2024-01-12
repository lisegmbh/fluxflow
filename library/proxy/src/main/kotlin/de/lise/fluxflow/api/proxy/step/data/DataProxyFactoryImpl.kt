package de.lise.fluxflow.api.proxy.step.data

import de.lise.fluxflow.api.proxy.ProxyBuilder
import de.lise.fluxflow.api.proxy.ProxyCreationException
import de.lise.fluxflow.api.step.stateful.StatefulStepDefinition
import de.lise.fluxflow.api.step.stateful.data.DataDefinition
import de.lise.fluxflow.api.step.stateful.data.StepDataService
import de.lise.fluxflow.stereotyped.step.data.DataKindInspector
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaMethod

class DataProxyFactoryImpl(
    private val stepDataService: StepDataService
) : DataProxyFactory {
    override fun <T> appendDataProxies(
        stepDefinition: StatefulStepDefinition,
        clazz: KClass<*>,
        builder: ProxyBuilder,
    ): ProxyBuilder {
        var result = builder

        clazz.memberProperties
            .filter { it.visibility == KVisibility.PUBLIC }
            .forEach { property ->
                val dataKind = DataKindInspector.getDataKind(property)
                val dataDefinition = stepDefinition.data.find { it.kind == dataKind }
                    ?: throw ProxyCreationException(
                        "Can not find step data kind '${dataKind.value}' in step '${stepDefinition.kind.value}'."
                    )

                result = appendDataProxy(
                    dataDefinition,
                    builder,
                    property
                )
            }

        return result
    }

    private fun <T> appendDataProxy(
        dataDefinition: DataDefinition<*>,
        builder: ProxyBuilder,
        property: KProperty1<T, *>,
    ): ProxyBuilder {
        var result = builder
        val dataKind = dataDefinition.kind

        property.getter.javaMethod?.let { getter ->
            result = result.method(ElementMatchers.named(getter.name))
                .intercept(
                    MethodDelegation.to(
                        DataGetterProxy(
                            stepDataService,
                            dataKind
                        )
                    )
                )
        }

        (property as? KMutableProperty1<T, *>)?.setter?.javaMethod?.let { setter ->
            if(!dataDefinition.isModifiable) {
                throw ProxyCreationException(
                    "Can not map setter of property '${property.name}' to step data '${dataKind.value}', as it is immutable."
                )
            }

            result = result.method(ElementMatchers.named(setter.name))
                .intercept(
                    MethodDelegation.to(
                        DataSetterProxy(
                            stepDataService,
                            dataKind
                        )
                    )
                )
        }

        return result
    }
}