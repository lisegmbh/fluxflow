package de.lise.fluxflow.springboot.rest.workflow.step

import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier

class StepNotFoundException(
    val workflowIdentifier: WorkflowIdentifier,
    val stepIdentifier: StepIdentifier
) : RuntimeException(
    "Step '${stepIdentifier.value}' could not be found for workflow '${workflowIdentifier.value}'."
)