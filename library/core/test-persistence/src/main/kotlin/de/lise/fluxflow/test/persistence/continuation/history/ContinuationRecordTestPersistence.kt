package de.lise.fluxflow.test.persistence.continuation.history

import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordData
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordPersistence
import de.lise.fluxflow.persistence.continuation.history.query.ContinuationRecordDataQuery
import de.lise.fluxflow.query.inmemory.filter.InMemoryPredicate
import de.lise.fluxflow.query.pagination.Page
import de.lise.fluxflow.test.persistence.CloningTestPersistence
import de.lise.fluxflow.test.persistence.TestIdGenerator
import de.lise.fluxflow.test.persistence.continuation.history.query.filter.ContinuationRecordTestFilter
import de.lise.fluxflow.test.persistence.continuation.history.query.sort.ContinuationRecordTestSort

class ContinuationRecordTestPersistence(
    private val idGenerator: TestIdGenerator,
    private val persistence: CloningTestPersistence<String, ContinuationRecordData> = CloningTestPersistence()
) : ContinuationRecordPersistence {
    override fun create(continuationRecord: ContinuationRecordData): ContinuationRecordData {
        if(continuationRecord.id != null) {
            throw IllegalArgumentException("The given record data seems to be already persisted, as the id is not null")
        }
        val persistedObject = continuationRecord.copy(id = idGenerator.newId())
        persistence[persistedObject.id!!] = persistedObject
        return persistedObject
    }

    override fun findAll(query: ContinuationRecordDataQuery): Page<ContinuationRecordData> {
        val predicate = query.filter?.let {
            ContinuationRecordTestFilter(it).toPredicate()
        } ?: InMemoryPredicate { true }

        val unsortedResult = persistence.all().filter { predicate.test(it) }

        val allResults = when(query.sort.isEmpty()) {
            true -> unsortedResult
            false -> unsortedResult.sortedWith(
                query.sort.map {
                    ContinuationRecordTestSort.toTestSort(it).toComparator()
                }.reduce{ acc, next ->
                    acc.thenComparing(next)
                }
            )
        }

        return Page.fromResult(
            allResults,
            query.page
        )
    }

    override fun deleteAllForWorkflow(identifierToDelete: WorkflowIdentifier) {
        persistence.all().filter {
            it.workflowId == identifierToDelete.value
        }.forEach {
            persistence.remove(it.id!!)
        }
    }
}