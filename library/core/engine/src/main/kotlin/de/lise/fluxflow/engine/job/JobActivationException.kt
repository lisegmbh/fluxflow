package de.lise.fluxflow.engine.job

class JobActivationException : Exception {
    constructor(id: String, kind: String) : super(
        "Unable to activate job #$id with kind '$kind'"
    )

    constructor(id: String, kind: String, cause: Throwable) : super(
        "Unable to activate job #$id with kind '$kind'",
        cause
    )
}
