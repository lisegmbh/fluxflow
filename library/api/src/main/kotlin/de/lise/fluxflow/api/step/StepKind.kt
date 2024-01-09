package de.lise.fluxflow.api.step

/**
 * A [StepKind] is used to distinguish between different types of steps.
 * In contrast to [StepIdentifier], it does not identify a concrete step.
 */
data class StepKind(
    val value: String
) {
    override fun toString(): String {
        return value
    }
}
