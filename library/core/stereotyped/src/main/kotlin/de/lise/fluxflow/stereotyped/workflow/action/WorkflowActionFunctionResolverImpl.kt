package de.lise.fluxflow.stereotyped.workflow.action

import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.reflection.activation.function.BasicFunctionResolver
import de.lise.fluxflow.reflection.activation.function.FunctionResolution
import de.lise.fluxflow.reflection.activation.parameter.CallbackParameterResolver
import de.lise.fluxflow.reflection.activation.parameter.ParamMatcher
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import de.lise.fluxflow.reflection.activation.parameter.PriorityParameterResolver
import de.lise.fluxflow.stereotyped.DefaultResolvers
import kotlin.reflect.KFunction

class WorkflowActionFunctionResolverImpl(
    private val basicParameterResolver: ParameterResolver
) : WorkflowActionFunctionResolver {
    override fun <TModel : Any> resolve(
        function: KFunction<*>,
        workflowProvider: () -> Workflow<TModel>,
    ): FunctionResolution<*> {
        val resolution = BasicFunctionResolver(
            PriorityParameterResolver(
                CallbackParameterResolver(
                    ParamMatcher.isFunctionInstance(),
                    { workflowProvider().model }
                ),
                *DefaultResolvers.forWorkflow(workflowProvider),
                basicParameterResolver
            )
        ).resolve(function)
        
        return resolution ?: throw WorkflowActionConfigurationException(
            "Could not resolve parameters for workflow action function '${function.name}'"
        )
    }
}