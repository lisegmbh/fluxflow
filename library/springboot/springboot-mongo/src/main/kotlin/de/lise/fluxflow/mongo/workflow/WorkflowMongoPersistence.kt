package de.lise.fluxflow.mongo.workflow

import de.fluxflow.flowquery.mapper.query.QueryMapper
import de.fluxflow.flowquery.query.FlowQuery
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.flowquery.repository.MongoQueryRepository
import de.lise.fluxflow.mongo.workflow.query.WorkflowDocumentQuery
import de.lise.fluxflow.persistence.workflow.WorkflowData
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import de.lise.fluxflow.persistence.workflow.query.WorkflowDataQuery
import de.lise.fluxflow.query.pagination.Page
import kotlin.jvm.optionals.getOrNull

class WorkflowMongoPersistence(
    private val repository: WorkflowRepository,
    private val queryableRepository: MongoQueryRepository<WorkflowDocument>,
    private val queryMapper: QueryMapper<WorkflowData, WorkflowDocument>
) : WorkflowPersistence {
    override fun create(model: Any?, forcedId: WorkflowIdentifier?): WorkflowData {
        return repository.insert(
            WorkflowDocument(
                forcedId?.value,
                model,
                model?.let { it::class.qualifiedName }
            )
        ).toWorkflowData()
    }

    override fun findAll(): List<WorkflowData> {
        return repository.findAll()
            .map { it.toWorkflowData() }
    }

    override fun findAll(query: WorkflowDataQuery): Page<WorkflowData> {
        return repository.findAll(
            WorkflowDocumentQuery(query)
        ).map { it.toWorkflowData() }
    }

    override fun findAll(query: FlowQuery<WorkflowData, WorkflowData>): Page<WorkflowData> {
        return queryableRepository.find(
            queryMapper.map(query)
        ).map {
            it.toWorkflowData()
        }
    }

    override fun find(id: WorkflowIdentifier): WorkflowData? {
        return repository.findById(id.value)
            .map { it.toWorkflowData() }
            .getOrNull()
    }

    override fun save(workflowData: WorkflowData): WorkflowData {
        return repository.save(
            WorkflowDocument(
                workflowData.id,
                workflowData.model,
                workflowData.model?.let { it::class.qualifiedName }
            )
        ).toWorkflowData()
    }

    override fun delete(id: WorkflowIdentifier) {
        repository.deleteById(id.value)
    }
}