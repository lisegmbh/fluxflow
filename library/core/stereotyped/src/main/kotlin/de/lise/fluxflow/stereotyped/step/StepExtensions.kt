package de.lise.fluxflow.stereotyped.step

import de.lise.fluxflow.api.step.Step

/**
 *
 */
fun <TStepModel> Step.bind(): TStepModel? {
    @Suppress("UNCHECKED_CAST")
    return (this as? ReflectedStatefulStep)?.instance as? TStepModel
}