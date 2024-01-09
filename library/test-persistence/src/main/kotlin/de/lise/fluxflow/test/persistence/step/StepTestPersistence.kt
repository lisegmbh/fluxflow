package de.lise.fluxflow.test.persistence.step

import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.StepPersistence
import de.lise.fluxflow.persistence.step.query.StepDataQuery
import de.lise.fluxflow.query.inmemory.filter.InMemoryPredicate
import de.lise.fluxflow.query.pagination.Page
import de.lise.fluxflow.test.persistence.TestIdGenerator
import de.lise.fluxflow.test.persistence.step.query.filter.StepTestFilter
import de.lise.fluxflow.test.persistence.step.query.sort.StepTestSort

class StepTestPersistence(
    private val idGenerator: TestIdGenerator = TestIdGenerator(),
    private val entities: MutableMap<String, StepData> = mutableMapOf()
) : StepPersistence {
    override fun randomId(): String {
        return idGenerator.newId()
    }

    override fun create(stepData: StepData): StepData {
        return save(stepData)
    }

    override fun findForWorkflow(workflowIdentifier: WorkflowIdentifier): List<StepData> {
        return entities.values.filter {
            it.workflowId == workflowIdentifier.value
        }
    }

    override fun findForWorkflow(workflowIdentifier: WorkflowIdentifier, query: StepDataQuery): Page<StepData> {
        val predicate = query.filter?.let {
            StepTestFilter(it).toPredicate()
        } ?: InMemoryPredicate { true }

        val unsortedResult = findForWorkflow(workflowIdentifier)
            .filter { predicate.test(it) }

        val allResults = when (query.sort.isEmpty()) {
            true -> unsortedResult
            false -> unsortedResult.sortedWith(
                query.sort.map {
                    StepTestSort.toTestSort(it).toComparator()
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

    override fun findForWorkflowAndId(
        workflowIdentifier: WorkflowIdentifier,
        stepIdentifier: StepIdentifier
    ): StepData? {
        return entities[stepIdentifier.value]?.takeIf {
            it.workflowId == workflowIdentifier.value
        }
    }

    override fun save(stepData: StepData): StepData {
        entities[stepData.id] = stepData
        return stepData
    }

    override fun deleteMany(stepIdentifiers: Set<StepIdentifier>) {
        stepIdentifiers.forEach {
            entities.remove(it.value)
        }
    }
}