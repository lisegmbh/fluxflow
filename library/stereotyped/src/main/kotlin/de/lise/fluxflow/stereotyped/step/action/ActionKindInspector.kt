package de.lise.fluxflow.stereotyped.step.action

import de.lise.fluxflow.api.step.stateful.action.ActionKind
import kotlin.reflect.KFunction


class ActionKindInspector private constructor(){
    companion object {
        internal fun getAnnotation(
            function: KFunction<*>,
        ): Action? {
            return function.annotations
                .firstNotNullOfOrNull {
                    it as? Action
                }
        }

        internal fun getActionKind(
            function: KFunction<*>,
            annotation: Action?
        ): ActionKind {
            return annotation?.value
                ?.takeUnless { it.isBlank() }
                ?.let { ActionKind(it) }
                ?: ActionKind(function.name)
        }

        fun getActionKind(
            function: KFunction<*>
        ): ActionKind {
            return getActionKind(
                function,
                getAnnotation(function)
            )
        }
    }
}