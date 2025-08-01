package de.lise.fluxflow.stereotyped.step.action

import de.lise.fluxflow.api.step.stateful.action.ActionKind
import kotlin.reflect.KFunction

class ActionKindInspector private constructor() {
    companion object {
        fun fromFunction(function: KFunction<*>): ActionKind {
            val actionAnnotation = function.annotations
                .firstNotNullOfOrNull { it as? Action }

            return actionAnnotation?.value
                ?.takeUnless { it.isBlank() }
                ?.let { ActionKind(it) }
                ?: ActionKind(function.name)
        }

        val KFunction<*>.actionKind: ActionKind
            get() {
                return fromFunction(this)
            }
    }
}