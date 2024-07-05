package de.lise.fluxflow.mongo.step

import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.query.filter.MongoEqualFilter
import de.lise.fluxflow.mongo.step.query.StepDocumentQuery
import de.lise.fluxflow.mongo.step.query.filter.StepDocumentFilter
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.StepPersistence
import de.lise.fluxflow.persistence.step.query.StepDataQuery
import de.lise.fluxflow.query.Query
import de.lise.fluxflow.query.pagination.Page
import org.bson.types.ObjectId
import org.springframework.data.domain.Sort
import kotlin.jvm.optionals.getOrNull

class StepMongoPersistence(
    private val stepRepository: StepRepository,
) : StepPersistence {

    override fun randomId(): String {
        return ObjectId.get()!!.toHexString()
    }

    override fun create(stepData: StepData): StepData {
        return stepRepository.insert(
            StepDocument(
                stepData.id,
                stepData.workflowId,
                stepData.kind,
                stepData.version,
                stepData.data,
                stepData.metadata,
                stepData.status,
            )
        ).toStepData()
    }

    override fun findForWorkflow(workflowIdentifier: WorkflowIdentifier): List<StepData> {
        return stepRepository.findByWorkflowId(workflowIdentifier.value)
            .map { it.toStepData() }
    }

    override fun findAll(query: StepDataQuery): Page<StepData> {
        val documentQuery = StepDocumentQuery(query)
        return stepRepository.findAll(
            documentQuery
        ).map { 
            it.toStepData()
        }
    }

    override fun findForWorkflow(workflowIdentifier: WorkflowIdentifier, query: StepDataQuery): Page<StepData> {
        val queryWithWorkflowFilter: Query<StepDocumentFilter, Sort> = StepDocumentQuery(query).mapFilter {
            StepDocumentFilter(
                id = it?.id,
                kind = it?.kind,
                version = it?.version,
                status = it?.status,
                workflowId = MongoEqualFilter(workflowIdentifier.value),
                metadata = it?.metadata
            )
        }

        return stepRepository.findAll(
            queryWithWorkflowFilter
        ).map {
            it.toStepData()
        }
    }

    override fun findForWorkflowAndId(
        workflowIdentifier: WorkflowIdentifier,
        stepIdentifier: StepIdentifier
    ): StepData? {
        return stepRepository.findByWorkflowIdAndId(
            workflowIdentifier.value,
            stepIdentifier.value,
        ).getOrNull()?.toStepData()
    }

    override fun save(stepData: StepData): StepData {
        return stepRepository.save(
            StepDocument(
                stepData.id,
                stepData.workflowId,
                stepData.kind,
                stepData.version,
                stepData.data,
                stepData.metadata,
                stepData.status
            )
        ).toStepData()
    }

    override fun deleteMany(stepIdentifiers: Set<StepIdentifier>) {
        stepRepository.deleteAllById(stepIdentifiers.map { it.value })
    }
}