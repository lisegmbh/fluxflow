package de.lise.fluxflow.mongo.step

import de.fluxflow.flowquery.mapper.query.QueryMapper
import de.fluxflow.flowquery.query.FlowQuery
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.flowquery.repository.MongoFlowQueryRepository
import de.lise.fluxflow.mongo.generic.ValueTypeConverter
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
    private val queryableRepository: MongoFlowQueryRepository<StepDocument>,
    private val queryMapper: QueryMapper<StepData, StepDocument>,
    private val valueTypes: ValueTypeConverter,
) : StepPersistence {

    constructor(
        stepRepository: StepRepository,
        queryableRepository: MongoFlowQueryRepository<StepDocument>,
        queryMapper: QueryMapper<StepData, StepDocument>,
    ) : this(stepRepository, queryableRepository, queryMapper, ValueTypeConverter.builtInsOnly())

    override fun randomId(): String {
        return ObjectId.get()!!.toHexString()
    }

    override fun create(stepData: StepData): StepData {
        val document = StepDocument(
            stepData.id,
            stepData.workflowId,
            stepData.kind,
            stepData.version,
            stepData.data,
            stepData.metadata,
            stepData.status,
        )
        document.toStepData(valueTypes)
        return stepRepository.insert(document).toStepData(valueTypes)
    }

    override fun findForWorkflow(workflowIdentifier: WorkflowIdentifier): List<StepData> {
        return stepRepository.findByWorkflowId(workflowIdentifier.value)
            .map { it.toStepData(valueTypes) }
    }

    override fun findAll(query: StepDataQuery): Page<StepData> {
        val documentQuery = StepDocumentQuery(query)
        return stepRepository.findAll(
            documentQuery
        ).map {
            it.toStepData(valueTypes)
        }
    }

    override fun findAll(query: FlowQuery<StepData, StepData>): Page<StepData> {
        return queryableRepository.find(
            queryMapper.map(query)
        ).map {
            it.toStepData(valueTypes)
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
            it.toStepData(valueTypes)
        }
    }

    override fun findForWorkflowAndId(
        workflowIdentifier: WorkflowIdentifier,
        stepIdentifier: StepIdentifier
    ): StepData? {
        return stepRepository.findByWorkflowIdAndId(
            workflowIdentifier.value,
            stepIdentifier.value,
        ).getOrNull()?.toStepData(valueTypes)
    }

    override fun save(stepData: StepData): StepData {
        val document = StepDocument(
            stepData.id,
            stepData.workflowId,
            stepData.kind,
            stepData.version,
            stepData.data,
            stepData.metadata,
            stepData.status,
        )
        document.toStepData(valueTypes)
        return stepRepository.save(document).toStepData(valueTypes)
    }

    override fun deleteMany(stepIdentifiers: Set<StepIdentifier>) {
        stepRepository.deleteAllById(stepIdentifiers.map { it.value })
    }
}
