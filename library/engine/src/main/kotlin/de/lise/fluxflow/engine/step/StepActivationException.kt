package de.lise.fluxflow.engine.step

class StepActivationException(id: String, kind: String) : Exception(
    "Unable to activate step #$id with kind '$kind'"
)