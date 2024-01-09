package de.lise.fluxflow.api.step

import de.lise.fluxflow.api.workflow.WorkflowIdentifier

data class WorkflowStepIdentifier(
    val workflowIdentifier: WorkflowIdentifier,
    val stepIdentifier: StepIdentifier
) {
    constructor(step: Step) : this(
        step.workflow.id,
        step.identifier
    )
}