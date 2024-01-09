package de.lise.fluxflow.engine.job

class JobActivationException(id: String, kind: String) : Exception(
    "Unable to activate job #$id with kind '$kind'"
)