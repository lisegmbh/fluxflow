package de.lise.fluxflow.api.continuation.history

import de.fluxflow.flowquery.service.ResourceQueryService
import de.lise.fluxflow.api.continuation.history.query.ContinuationRecordQuery
import de.lise.fluxflow.api.continuation.history.query.ContinuationRecordQueryable
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.query.pagination.Page

interface ContinuationHistoryService : ResourceQueryService<ContinuationRecord, ContinuationRecordQueryable> {
    @Deprecated("Use the new FlowQuery overloads instead.")
    fun findAll(query: ContinuationRecordQuery): Page<ContinuationRecord>
    
    fun findPreviousStep(currentStep: Step): Step?
    fun deleteAllForWorkflow(identifierToDelete: WorkflowIdentifier)
}