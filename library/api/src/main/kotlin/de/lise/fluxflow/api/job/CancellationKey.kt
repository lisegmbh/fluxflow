package de.lise.fluxflow.api.job

/**
 * A [CancellationKey] can be assigned to jobs.
 * Whenever a new job is scheduled, that has the same cancellation key, the old job will be canceled/replaced.
 * @param value This key's string representation.
 */
data class CancellationKey(
    val value: String
)
