package de.lise.fluxflow.stereotyped.step

import de.lise.fluxflow.api.step.StepKind
import kotlin.reflect.KClass

class StepKindInspector private constructor() {
    companion object {
        @Deprecated(
            "Will be replaced by .fromClass",
            ReplaceWith("fromClass(type)")
        )
        fun getStepKind(type: KClass<*>): StepKind {
            return fromClass(type)
        }

        fun fromClass(type: KClass<*>): StepKind {
            val stepAnnotation = type.annotations
                .firstNotNullOfOrNull { it as? Step }

            return stepAnnotation?.takeUnless { it.kind.isBlank() }
                ?.let { StepKind(it.kind) }
                ?: StepKind(type.qualifiedName!!)
        }
    }
}