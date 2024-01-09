package de.lise.fluxflow.api.job

data class JobIdentifier(
    val value: String
) {
    override fun toString(): String {
        return value
    }
}