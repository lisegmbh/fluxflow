package de.lise.fluxflow.stereotyped.step.automation

import de.lise.fluxflow.api.step.automation.AutomationDefinition
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import de.lise.fluxflow.stereotyped.continuation.ContinuationBuilder
import de.lise.fluxflow.stereotyped.step.action.ImplicitStatusBehavior
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility


class AutomationDefinitionBuilder(
    private val continuationBuilder: ContinuationBuilder,
    private val parameterResolver: ParameterResolver
) {
    
    fun isAutomation(function: KFunction<*>): Boolean {
        return function.annotations.any {
            isAutomationAnnotation(it)
        }
    }
    
    fun <T : Any> build(
        function: KFunction<*>
    ): Map<Trigger, ((T) -> AutomationDefinition)> {
        if (
            function.visibility != KVisibility.PUBLIC ||
            function.isAbstract
        ) {
            throw AutomationConfigurationException(
                "Can not use function '${function.name}' for automation as it is not public, abstract or requires parameters"
            )
        }

        val triggers = getTriggers(function)
        val converter = continuationBuilder.createResultConverter(
            ImplicitStatusBehavior.Preserve, 
            false, 
            function
        )

        val definitionCreator: (T) -> AutomationDefinition = { obj ->
            ReflectedAutomationDefinition(
                obj
            ) { currentStep, instance ->
                val callable = AutomationFunctionResolver(
                    parameterResolver,
                    function,
                    { instance },
                    { currentStep },
                ).resolve()
                converter.toContinuation(callable.call())
            }
        }

        return triggers.associateWith { definitionCreator }
    }

    private fun getTriggers(function: KFunction<*>): Set<Trigger> {
        return function.annotations.mapNotNull {
            when(it) {
                is Automated -> it.trigger
                is OnCreated -> Trigger.OnCreated
                is OnCompleted -> Trigger.OnCompleted
                else -> null
            }
        }.toSet()
    }

    companion object {
        fun isAutomationAnnotation(annotation: Annotation): Boolean {
            return annotation is Automated
                    || annotation is OnCreated
                    || annotation is OnCompleted
        }
    }
}