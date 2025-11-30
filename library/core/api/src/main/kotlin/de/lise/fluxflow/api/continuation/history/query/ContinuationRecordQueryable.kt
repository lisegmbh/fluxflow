package de.lise.fluxflow.api.continuation.history.query

import de.fluxflow.flowquery.expression.Expression
import de.lise.fluxflow.api.WorkflowObjectReference
import de.lise.fluxflow.api.continuation.ContinuationType
import de.lise.fluxflow.api.continuation.history.ContinuationRecordIdentifier
import de.lise.fluxflow.api.continuation.reason.Reason
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import java.time.Instant

interface ContinuationRecordQueryable {
    val id: ContinuationRecordIdentifier
    val workflowIdentifier: WorkflowIdentifier
    val timeOfOccurrence: Instant
    val type: ContinuationType
    val originatingObject: WorkflowObjectReference?
    val targetObject: WorkflowObjectReference?
    val reason: Reason?

    companion object {
        val <TRoot> Expression<TRoot, ContinuationRecordQueryable>.id
            get() = this.get(ContinuationRecordQueryable::id)

        val <TRoot> Expression<TRoot, ContinuationRecordQueryable>.workflowIdentifier
            get() = this.get(ContinuationRecordQueryable::workflowIdentifier)

        val <TRoot> Expression<TRoot, ContinuationRecordQueryable>.type
            get() = this.get(ContinuationRecordQueryable::type)

        val <TRoot> Expression<TRoot, ContinuationRecordQueryable>.originatingObject
            get() = this.get(ContinuationRecordQueryable::originatingObject)

        val <TRoot> Expression<TRoot, ContinuationRecordQueryable>.targetObject
            get() = this.get(ContinuationRecordQueryable::targetObject)

        val <TRoot> Expression<TRoot, ContinuationRecordQueryable>.reason
            get() = this.get(ContinuationRecordQueryable::reason)
    }
}