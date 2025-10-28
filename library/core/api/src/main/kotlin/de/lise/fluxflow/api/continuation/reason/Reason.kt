package de.lise.fluxflow.api.continuation.reason

/**
 * A [Reason] provides context about why a specific continuation was chosen during workflow execution.
 * It contains a human-readable message and optional structured context data.
 *
 * @param message A human-readable explanation of why this continuation was chosen.
 * @param context Additional structured data providing context for the reason.
 */
data class Reason(
    val message: String,
    val context: Map<String, Any?> = emptyMap()
)