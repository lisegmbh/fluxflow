package de.lise.fluxflow.api.job.parameter

data class ParameterKind(
    val value: String
) {
    override fun toString(): String {
        return value
    }
}
