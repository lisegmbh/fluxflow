@file:Suppress("unused", "CanBeParameter") // Used by DTOs/serialization

package de.lise.fluxflow.springboot.rest

import de.lise.fluxflow.api.workflow.WorkflowNotFoundException
import de.lise.fluxflow.springboot.rest.workflow.step.StepNotFoundException

sealed class FluxFlowExceptionDto(
    val kind: String,
    val reason: String
) {
    class WorkflowNotFound(
        val workflowIdentifier: String
    ) : FluxFlowExceptionDto(
        "workflowNotFound",
        "Workflow with id '${workflowIdentifier}' could not be found."
    ) {
        constructor(
            ex: WorkflowNotFoundException
        ) : this(
            ex.identifier.value
        )
    }

    class StepNotFound(
        val workflowIdentifier: String,
        val stepIdentifier: String
    ) : FluxFlowExceptionDto(
        "stepNotFound",
        "Step with id '${stepIdentifier}' could not be found for workflow '${workflowIdentifier}'."
    ) {
        constructor(
            ex: StepNotFoundException
        ) : this(
            ex.workflowIdentifier.value,
            ex.stepIdentifier.value
        )
    }
}
