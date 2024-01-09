package de.lise.fluxflow.stereotyped.step.action

import de.lise.fluxflow.api.step.stateful.action.Action
import de.lise.fluxflow.api.step.stateful.action.ActionDefinition
import de.lise.fluxflow.api.step.stateful.action.ActionKind
import de.lise.fluxflow.api.validation.ValidationBehavior
import de.lise.fluxflow.stereotyped.continuation.ContinuationBuilder
import de.lise.fluxflow.stereotyped.metadata.MetadataBuilder
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.functions

/**
 * The [ActionDefinitionBuilder] can be used to build an [Action] using reflection.
 */
class ActionDefinitionBuilder(
    private val continuationBuilder: ContinuationBuilder,
    private val metadataBuilder: MetadataBuilder,
    private val actionFunctionResolver: ActionFunctionResolver
) {
    /**
     * Checks if the given type has at least on method annotated with [Action].
     * @return `true`, if at least on methods of [type] is annotated with [Action]
     */
    fun <T : Any> hasAnnotatedAction(type: KClass<out T>): Boolean {
        return type.functions
            .flatMap { it.annotations }
            .any { isActionAnnotation(it) }
    }

    /**
     * This method returns a builder function that
     * can be used create actions executing the given function.
     *
     * @param function The function that should be invoked when [Action.execute] is called.
     * @param requireAnnotation If set to `true`, the given function must be annotated with [Action].
     * @param TFunctionOwner The type declaring the given [function].
     * @return A builder that creates an [Action], which will invoke the given [function].
     * If the given function cannot be used invoked by an action (because is static, private, etc.)
     * or isn't annotated with [Action] while [requireAnnotation] is set to `true`,
     * `null` is returned.
     */
    fun <TFunctionOwner : Any> build(
        function: KFunction<*>,
        requireAnnotation: Boolean,
    ): ((TFunctionOwner) -> ActionDefinition)? {
        if (
            !canBeInvoked(function)
        ) {
            return null
        }

        val annotation = function.annotations
            .firstNotNullOfOrNull { it as? de.lise.fluxflow.stereotyped.step.action.Action }
        if (requireAnnotation && annotation == null) {
            return null
        }

        if (
            annotation == null && hasAdditionalParameters(function)
        ) {
            return null
        }

        val implicitStatusBehavior = annotation?.statusBehavior ?: ImplicitStatusBehavior.Default
        val kind = annotation?.value
            ?.takeUnless { it.isBlank() }
            ?.let { ActionKind(it) }
            ?: ActionKind(function.name)
        val metadata = metadataBuilder.build(function)
        val converter = continuationBuilder.createResultConverter(implicitStatusBehavior, true, function)

        val beforeExecutionBehavior = annotation?.beforeExecutionValidation ?: ValidationBehavior.Default
        val validationGroups = annotation?.validationGroups?.let { setOf(*it) } ?: emptySet()
        
        return { obj ->
            ReflectedActionDefinition(
                kind,
                metadata,
                beforeExecutionBehavior,
                validationGroups,
                obj
            ) { step, instance ->
                val callable = actionFunctionResolver.resolve(
                    function,
                    { instance },
                    { step }
                )

                converter.toContinuation(callable.call()) }
        }
    }

    companion object {
        fun isActionAnnotation(annotation: Annotation): Boolean {
            return annotation is de.lise.fluxflow.stereotyped.step.action.Action
        }

        private fun canBeInvoked(function: KFunction<*>): Boolean {
            return function.visibility == KVisibility.PUBLIC && !function.isAbstract
        }

        private fun hasAdditionalParameters(function: KFunction<*>): Boolean {
            return function.parameters.any { it.kind != KParameter.Kind.INSTANCE }
        }

    }

}