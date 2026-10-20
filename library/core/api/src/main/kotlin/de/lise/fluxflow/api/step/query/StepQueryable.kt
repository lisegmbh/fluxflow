package de.lise.fluxflow.api.step.query

import de.fluxflow.flowquery.expression.Expression
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

    companion object {
        val <TRoot> Expression<TRoot, StepQueryable>.identifier
            get() = this.get(StepQueryable::identifier)

        val <TRoot> Expression<TRoot, StepQueryable>.workflowIdentifier
            get() = this.get(StepQueryable::workflowIdentifier)

        val <TRoot> Expression<TRoot, StepQueryable>.kind
            get() = this.get(StepQueryable::kind)

        val <TRoot> Expression<TRoot, StepQueryable>.data
            get() = this.get(StepQueryable::data)

        val <TRoot> Expression<TRoot, StepQueryable>.status
            get() = this.get(StepQueryable::status)

        val <TRoot> Expression<TRoot, StepQueryable>.metadata
            get() = this.get(StepQueryable::metadata)
    }
}