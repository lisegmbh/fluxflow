package de.lise.fluxflow.api.proxy.step.action

import de.lise.fluxflow.api.proxy.KotlinElementMatchers.Companion.elementMatcher
import de.lise.fluxflow.api.proxy.ProxyBuilder
import de.lise.fluxflow.api.proxy.ProxyCreationException
import de.lise.fluxflow.api.step.stateful.StatefulStepDefinition
import de.lise.fluxflow.api.step.stateful.action.ActionDefinition
import de.lise.fluxflow.api.step.stateful.action.ActionService
import de.lise.fluxflow.stereotyped.step.action.ActionKindInspector
import net.bytebuddy.implementation.MethodDelegation
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.javaMethod

class ActionProxyFactoryImpl(
    private val actionService: ActionService
) : ActionProxyFactory {

    override fun <T : Any> appendActionProxies(
        stepDefinition: StatefulStepDefinition,
        clazz: KClass<T>,
        parentBuilder: ProxyBuilder
    ): ProxyBuilder {
        var result = parentBuilder

        clazz.memberFunctions
            .filter { it.visibility == KVisibility.PUBLIC }
            .filter { it.javaMethod?.declaringClass != Object::class.java }
            .forEach { function ->
                val actionKind = ActionKindInspector.getActionKind(function)
                val actionDefinition = stepDefinition.actions.find { it.kind == actionKind }
                    ?: throw ProxyCreationException(
                        "Can not find the step action '${actionKind.value}' in step '${stepDefinition.kind.value}'."
                    )

                result = appendActionProxy(
                    actionDefinition,
                    result,
                    function
                )
            }

        return result
    }

    private fun appendActionProxy(
        actionDefinition: ActionDefinition,
        parentBuilder: ProxyBuilder,
        function: KFunction<*>
    ): ProxyBuilder {
        var result = parentBuilder
        val actionKind = actionDefinition.kind

        result = result.method(function.elementMatcher())
            .intercept(
                MethodDelegation.to(
                    ActionInvocationProxy(
                        actionService,
                        actionKind
                    )
                )
            )

        return result
    }
}