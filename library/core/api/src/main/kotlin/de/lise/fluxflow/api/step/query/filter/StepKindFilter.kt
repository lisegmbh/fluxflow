package de.lise.fluxflow.api.step.query.filter

import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.query.filter.Filter

data class StepKindFilter(
    val value: Filter<String>
) {
    companion object {
        fun eq(kind: StepKind): StepKindFilter {
            return StepKindFilter(
                Filter.eq(kind.value)
            )
        }

        fun anyOf(kinds: Collection<StepKind>): StepKindFilter {
            return StepKindFilter(
                Filter.anyOf(
                    kinds.map { it.value }
                )
            )
        }
    }
}