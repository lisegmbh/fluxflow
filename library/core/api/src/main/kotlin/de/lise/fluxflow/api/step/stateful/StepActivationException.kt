package de.lise.fluxflow.api.step.stateful

/**
 * This exception is thrown whenever a step's state could not be restored/reactivated.
 */
open class StepActivationException : Exception {
    constructor(message: String, cause: Throwable) : super(
        message,
        cause
    )

    constructor(message: String) : super(message)

    constructor(id: String, kind: String) : super(
        "Unable to activate step #$id with kind '$kind'"
    )
}