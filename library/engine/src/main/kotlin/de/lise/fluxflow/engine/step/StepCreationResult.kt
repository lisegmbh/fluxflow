package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.Step

data class StepCreationResult(
    val step: Step,
    val nextContinuations: Collection<Continuation<*>>
)