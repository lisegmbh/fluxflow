package de.lise.fluxflow.api.proxy

import de.lise.fluxflow.api.proxy.step.StepAccessor
import de.lise.fluxflow.api.proxy.step.StepProxyType
import de.lise.fluxflow.api.proxy.step.data.DataProxyFactory
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepDefinition
import de.lise.fluxflow.api.step.stateful.StatefulStepDefinition
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.FieldAccessor
import net.bytebuddy.implementation.MethodCall
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaMethod


class ProxyTypeFactoryImpl(
    private val dataProxyFactory: DataProxyFactory,
) : ProxyTypeFactory {
    override fun <T : Any> createProxy(
        stepDefinition: StepDefinition,
        clazz: KClass<T>
    ): StepProxyType<T> {
        if (clazz.isFinal) {
            throw ProxyCreationException.finalClass(clazz)
        }
        if (!clazz.java.isInterface) {
            throw InvalidProxyInterfaceException("Proxying currently only supports interfaces.")
        }

        var builder: DynamicType.Builder<out Any> = when (clazz.java.isInterface) {
            true -> ByteBuddy().subclass(Any::class.java).implement(clazz.java)
            false -> ByteBuddy().subclass(clazz.java)
        }.implement(StepAccessor::class.java)
            .defineField(STEP_FIELD_NAME, Step::class.java, Modifier.PRIVATE.and(Modifier.FINAL))
            .defineMethod(STEP_GETTER_NAME, Step::class.java, Modifier.PUBLIC)
            .intercept(FieldAccessor.ofField(STEP_FIELD_NAME))
            .defineConstructor(Visibility.PUBLIC)
            .withParameters(Step::class.java)
            .intercept(
                MethodCall.invoke(Any::class.primaryConstructor!!.javaConstructor!!).andThen(
                    FieldAccessor.ofField(STEP_FIELD_NAME).setsArgumentAt(0)
                )
            )


        if (stepDefinition is StatefulStepDefinition) {
            builder = dataProxyFactory.appendDataProxies(
                stepDefinition,
                clazz,
                builder
            )
        } else {
            val unmappedProperties = clazz.memberProperties.filter { it.visibility == KVisibility.PUBLIC }
            if (unmappedProperties.any()) {
                throw ProxyCreationException(
                    "Could not map the following properties, as the given step (${stepDefinition.kind.value}) is not stateful: ${
                        unmappedProperties.joinToString(", ") { prop -> prop.name }
                    }"
                )
            }
        }

        val targetType = builder.make().load(Thread.currentThread().contextClassLoader).loaded
        val constructor = targetType.constructors.find { it.parameterCount == 1 }!!


        return StepProxyType { step ->
            @Suppress("UNCHECKED_CAST")
            constructor.newInstance(step) as T
        }
    }

    private companion object {
        val STEP_FIELD_NAME: String = StepAccessor::_proxyStep.name
        val STEP_GETTER_NAME: String = StepAccessor::_proxyStep.getter.javaMethod!!.name
    }
}