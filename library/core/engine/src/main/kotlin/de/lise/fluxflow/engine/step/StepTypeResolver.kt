package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.step.StepKind
import kotlin.reflect.KClass

/**
 * The [StepTypeResolver] can be used to resolve a step's type based on its kind.
 */
interface StepTypeResolver {
    /**
     * Resolves the step's type based on its kind.
     *
     * @param kind The kind of the step.
     * @return The type of the step.
     * @throws ClassNotFoundException If the type cannot be resolved.
     */
    fun resolveType(kind: StepKind): KClass<out Any>
}