package de.lise.fluxflow.api.continuation.history

import de.fluxflow.flowquery.query.FlowQuery
import de.fluxflow.flowquery.query.FlowQueryBuilder
import de.lise.fluxflow.api.continuation.history.query.ContinuationRecordQuery
import de.lise.fluxflow.api.continuation.history.query.ContinuationRecordQueryable
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.query.pagination.Page

interface ContinuationHistoryService {
    @Deprecated("Use the new FlowQuery overloads instead.")
    fun findAll(query: ContinuationRecordQuery): Page<ContinuationRecord>
    
    fun findAll(
        query: FlowQuery<ContinuationRecordQueryable, ContinuationRecordQueryable>
    ): Page<ContinuationRecord>
    fun findAll(
        builder: FlowQueryBuilder<ContinuationRecordQueryable, ContinuationRecordQueryable>
    ): Page<ContinuationRecord> {
        return findAll(
            builder(
                FlowQuery.of()
            )
        )
    }
    
    fun findPreviousStep(currentStep: Step): Step?
    fun deleteAllForWorkflow(identifierToDelete: WorkflowIdentifier)
}