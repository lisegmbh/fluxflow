package de.lise.fluxflow.stereotyped.step.data.validation

import de.lise.fluxflow.api.step.stateful.data.validation.DataValidationDefinition

@Deprecated("Will be removed")
fun interface ValidationBuilderResult<TInstance> {
    fun build(instance: TInstance): DataValidationDefinition
}