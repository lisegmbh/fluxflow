package de.lise.fluxflow.api.step.stateful.action

import de.lise.fluxflow.api.step.Step

class ActionNotFoundException(
    val step: Step,
    val kind: ActionKind
): Exception(
    "Step action of kind '$kind' could not be found for step (id: '${
        step.identifier
    }', kind: '${
        step.definition.kind
    }')"
)