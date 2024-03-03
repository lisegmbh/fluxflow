package de.lise.fluxflow.test.persistence.workflow

import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.persistence.workflow.WorkflowData
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import de.lise.fluxflow.persistence.workflow.query.WorkflowDataQuery
import de.lise.fluxflow.query.inmemory.filter.InMemoryPredicate
import de.lise.fluxflow.query.pagination.Page
import de.lise.fluxflow.test.persistence.CloningTestPersistence
import de.lise.fluxflow.test.persistence.TestIdGenerator
import de.lise.fluxflow.test.persistence.workflow.query.filter.WorkflowTestFilter
import de.lise.fluxflow.test.persistence.workflow.query.sort.WorkflowTestSort

class WorkflowTestPersistence(
    private val idGenerator: TestIdGenerator = TestIdGenerator(0),
    private val persistence: CloningTestPersistence<String, WorkflowData> = CloningTestPersistence()
) : WorkflowPersistence {
    override fun create(model: Any?, forcedId: WorkflowIdentifier?): WorkflowData {
        return save(
            WorkflowData(
                forcedId?.value ?: idGenerator.newId(),
                model
            )
        )
    }

    override fun findAll(): List<WorkflowData> {
        return persistence.all()
    }

    override fun findAll(query: WorkflowDataQuery): Page<WorkflowData> {
        val predicate = query.filter?.let {
            WorkflowTestFilter(it).toPredicate()
        } ?: InMemoryPredicate { true }

        val unsortedResult = findAll()
            .filter { predicate.test(it) }

        val allResults = when (query.sort.isEmpty()) {
            true -> unsortedResult
            false -> unsortedResult.sortedWith(
                query.sort.map {
                    WorkflowTestSort.toTestSort(it).toComparator()
                }.reduce { acc, next ->
                    acc.thenComparing(next)
                }
            )
        }

        return Page.fromResult(
            allResults,
            query.page
        )
    }

    override fun find(id: WorkflowIdentifier): WorkflowData? {
        return persistence[id.value]
    }

    override fun save(workflowData: WorkflowData): WorkflowData {
        persistence[workflowData.id] = workflowData
        return workflowData
    }

    override fun delete(id: WorkflowIdentifier) {
        persistence.remove(id.value)
    }
}