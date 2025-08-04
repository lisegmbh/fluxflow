package de.lise.fluxflow.stereotyped.workflow.action

import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.reflection.activation.function.FunctionResolution
import kotlin.reflect.KFunction

interface WorkflowActionFunctionResolver {
    fun <TModel : Any> resolve(
        function: KFunction<*>,
        workflowProvider: () -> Workflow<TModel>
    ): FunctionResolution<*>
}