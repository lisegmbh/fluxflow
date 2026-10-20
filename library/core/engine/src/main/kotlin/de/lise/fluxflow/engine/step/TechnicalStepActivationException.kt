package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.step.stateful.StepActivationException

/** Marks a technical activation failure that requires opting in to general exception restoration. */
internal class TechnicalStepActivationException(
    id: String,
    kind: String,
    cause: Throwable,
) : StepActivationException(id, kind, cause)
