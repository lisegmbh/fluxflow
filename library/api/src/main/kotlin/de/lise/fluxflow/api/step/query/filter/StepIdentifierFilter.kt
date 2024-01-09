package de.lise.fluxflow.api.step.query.filter

import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.query.filter.Filter

data class StepIdentifierFilter(
    val value: Filter<String>
) {
    companion object {
        fun eq(stepIdentifier: StepIdentifier): StepIdentifierFilter {
            return StepIdentifierFilter(
                Filter.eq(stepIdentifier.value)
            )
        }

        fun anyOf(stepIdentifiers: Collection<StepIdentifier>): StepIdentifierFilter {
            return StepIdentifierFilter(
                Filter.anyOf(
                    stepIdentifiers.map { it.value }
                )
            )
        }
    }
}