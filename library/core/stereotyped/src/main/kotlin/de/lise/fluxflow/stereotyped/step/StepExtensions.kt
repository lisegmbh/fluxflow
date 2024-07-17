package de.lise.fluxflow.stereotyped.step

import de.lise.fluxflow.api.step.Step

/**
 * 
 */
fun <TStepModel> Step.bind(): TStepModel? {
        val definition = this.definition as? ReflectedStatefulStepDefinition ?: return null
        
        @Suppress("UNCHECKED_CAST")
        return definition.instance as? TStepModel
}