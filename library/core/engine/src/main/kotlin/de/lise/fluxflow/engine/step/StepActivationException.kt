package de.lise.fluxflow.engine.step

class StepActivationException : Exception {
    constructor(message: String, cause: Throwable) : super(
        message,
        cause
    )

    constructor(id: String, kind: String) : super(
        "Unable to activate step #$id with kind '$kind'"
    )
}