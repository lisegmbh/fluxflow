package de.lise.fluxflow.api.step.stateful.data

import de.lise.fluxflow.api.step.Step

data class DataNotFoundException(
    val step: Step,
    val kind: DataKind
) : Exception(
    "Step data of kind '$kind' could not be found for step (id: '${
        step.identifier
    }', kind: '${
        step.definition.kind
    }')"
)