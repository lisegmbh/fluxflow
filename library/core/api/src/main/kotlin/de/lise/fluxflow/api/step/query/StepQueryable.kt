package de.lise.fluxflow.api.step.query

import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.workflow.WorkflowIdentifier

interface StepQueryable {
    val identifier: StepIdentifier
    val workflowIdentifier: WorkflowIdentifier
    val kind: StepKind
    val version: String
    val data: Map<String, Any?>
    val status: Status
    val metadata: Map<String, Any>
}