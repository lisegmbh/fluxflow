package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.stereotyped.step.ReflectedStatefulStepDefinition

fun <TStepModel> Step.bind(): TStepModel? {
    val definition = this.definition as? ReflectedStatefulStepDefinition ?: return null

    @Suppress("UNCHECKED_CAST")
    return definition.instance as? TStepModel
}