package de.lise.fluxflow.api.continuation.history

import de.lise.fluxflow.api.continuation.history.query.ContinuationRecordQuery
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.query.pagination.Page

interface ContinuationHistoryService {
    fun findAll(query: ContinuationRecordQuery): Page<ContinuationRecord>
    fun findPreviousStep(currentStep: Step): Step?
}