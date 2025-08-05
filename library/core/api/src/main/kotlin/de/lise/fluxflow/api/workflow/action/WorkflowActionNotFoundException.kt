package de.lise.fluxflow.api.workflow.action

import de.lise.fluxflow.api.step.stateful.action.ActionKind
import de.lise.fluxflow.api.workflow.Workflow

class WorkflowActionNotFoundException(
    val workflow: Workflow<*>,
    val kind: ActionKind
): Exception(
    "Workflow action of kind '$kind' could not be found for workflow ${
        workflow.identifier
    } (available kinds: ${
        workflow.definition.actions
            .map { it.kind }
            .joinToString(", ")
    })"
)