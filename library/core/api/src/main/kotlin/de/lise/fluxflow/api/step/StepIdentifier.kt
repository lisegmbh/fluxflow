package de.lise.fluxflow.api.step

/**
 * A [StepIdentifier] is used to identify an actual step instance.
 */
data class StepIdentifier(
    val value: String
) {
    override fun toString(): String {
        return value
    }
}