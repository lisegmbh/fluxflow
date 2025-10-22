package de.lise.fluxflow.api.continuation.history.query

import de.fluxflow.flowquery.expression.Expression
import de.lise.fluxflow.api.WorkflowObjectReference
import de.lise.fluxflow.api.continuation.ContinuationType
import de.lise.fluxflow.api.continuation.history.ContinuationRecordIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import java.time.Instant

interface ContinuationRecordQueryable {
    val id: ContinuationRecordIdentifier
    val workflowIdentifier: WorkflowIdentifier
    val timeOfOccurrence: Instant
    val type: ContinuationType
    val originatingObject: WorkflowObjectReference?
    val targetObject: WorkflowObjectReference?

    companion object {
        val <TRoot> Expression<TRoot, ContinuationRecordQueryable>.id: Expression<TRoot, ContinuationRecordIdentifier>
            get() = this.get(ContinuationRecordQueryable::id)

        val <TRoot> Expression<TRoot, ContinuationRecordQueryable>.workflowIdentifier: Expression<TRoot, WorkflowIdentifier>
            get() = this.get(ContinuationRecordQueryable::workflowIdentifier)

        val <TRoot> Expression<TRoot, ContinuationRecordQueryable>.type: Expression<TRoot, ContinuationType>
            get() = this.get(ContinuationRecordQueryable::type)

        val <TRoot> Expression<TRoot, ContinuationRecordQueryable>.originatingObject: Expression<TRoot, WorkflowObjectReference?>
            get() = this.get(ContinuationRecordQueryable::originatingObject)

        val <TRoot> Expression<TRoot, ContinuationRecordQueryable>.targetObject: Expression<TRoot, WorkflowObjectReference?>
            get() = this.get(ContinuationRecordQueryable::targetObject)
    }
}