package de.lise.fluxflow.api.job

/**
 * A [JobKind] is used to distinguish between different types of jobs.
 * IN contrast to [JobIdentifier], it does not identify a concrete job.
 */
class JobKind(
    val value: String
) {
    override fun toString(): String {
        return value
    }
}