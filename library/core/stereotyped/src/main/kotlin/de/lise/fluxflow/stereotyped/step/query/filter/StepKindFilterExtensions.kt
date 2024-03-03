package de.lise.fluxflow.stereotyped.step.query.filter

import de.lise.fluxflow.api.step.query.filter.StepKindFilter
import de.lise.fluxflow.stereotyped.step.StepKindInspector
import kotlin.reflect.KClass

fun StepKindFilter.Companion.eq(type: KClass<*>): StepKindFilter {
    return eq(
        StepKindInspector.getStepKind(type)
    )
}

fun StepKindFilter.Companion.anyOf(types: Collection<KClass<*>>): StepKindFilter {
    return anyOf(
        types.map { StepKindInspector.getStepKind(it) }
    )
}