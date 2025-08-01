package de.lise.fluxflow.stereotyped.workflow.action

import de.lise.fluxflow.api.step.stateful.action.ActionKind
import de.lise.fluxflow.api.workflow.action.WorkflowActionDefinition
import de.lise.fluxflow.reflection.hasAdditionalParameters
import de.lise.fluxflow.reflection.isInvokableInstanceFunction
import de.lise.fluxflow.stereotyped.continuation.ContinuationBuilder
import de.lise.fluxflow.stereotyped.metadata.MetadataBuilder
import de.lise.fluxflow.stereotyped.step.action.Action
import de.lise.fluxflow.stereotyped.step.action.ImplicitStatusBehavior
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.functions

class WorkflowActionDefinitionBuilder(
    private val metadataBuilder: MetadataBuilder,
    private val continuationBuilder: ContinuationBuilder,
    private val actionFunctionResolver: WorkflowActionFunctionResolver,
) {
    fun <TModel : Any> build(
        type: KClass<out TModel>,
    ): List<WorkflowActionDefinition<TModel>> {
        return type.functions.mapNotNull {
            build(
                it,
                true
            )
        }
    }

    private fun <TModel : Any> build(
        function: KFunction<*>,
        requireAnnotation: Boolean,
    ): WorkflowActionDefinition<TModel>? {
        if (
            !function.isInvokableInstanceFunction()
        ) {
            return null
        }

        val annotation = function.annotations
            .firstNotNullOfOrNull { it as? Action }
        if (requireAnnotation && annotation == null) {
            return null
        }

        if (
            annotation == null && function.hasAdditionalParameters()
        ) {
            return null
        }

        val kind = annotation?.value
            ?.takeUnless { it.isBlank() }
            ?.let { ActionKind(it) }
            ?: ActionKind(function.name)
        val metadata = metadataBuilder.build(function)
        val converter = continuationBuilder.createResultConverter(
            ImplicitStatusBehavior.Preserve,
            false,
            function
        )

        return ReflectedWorkflowActionDefinition(
            kind,
            metadata
        ) { workflow ->
            val callable = actionFunctionResolver.resolve(
                function
            ) { workflow }

            converter.toContinuation(callable.call())
        }
    }
}