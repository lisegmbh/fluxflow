package de.lise.fluxflow.mongo.workflow

import de.fluxflow.flowquery.mapper.query.QueryMapper
import de.fluxflow.flowquery.mapper.query.QueryMapperImpl
import de.lise.fluxflow.mongo.ConditionalOnFluxFlowMongo
import de.lise.fluxflow.mongo.flowquery.repository.MongoExecutor
import de.lise.fluxflow.mongo.flowquery.repository.MongoFlowQueryRepository
import de.lise.fluxflow.mongo.flowquery.repository.MongoQueryTranslator
import de.lise.fluxflow.mongo.workflow.flowquery.WorkflowDataToDocumentReplacer
import de.lise.fluxflow.persistence.workflow.WorkflowData
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate

@Configuration
@ConditionalOnFluxFlowMongo
open class WorkflowMongoConfiguration {
    @Bean
    internal open fun workflowMongoExecutor(mongoTemplate: MongoTemplate): MongoExecutor<WorkflowDocument> {
        return MongoExecutor(mongoTemplate, WorkflowDocument::class.java)
    }

    @Bean
    internal open fun workflowFlowQueryRepository(
        queryTranslator: MongoQueryTranslator,
        executor: MongoExecutor<WorkflowDocument>,
    ): MongoFlowQueryRepository<WorkflowDocument> {
        return MongoFlowQueryRepository(
            WorkflowDocument::class, queryTranslator, executor
        )
    }

    @Bean
    open fun workflowDocumentMapper(): QueryMapper<WorkflowData, WorkflowDocument> {
        return QueryMapperImpl(
            WorkflowDataToDocumentReplacer().toMapper()
        )
    }

    @Bean
    open fun workflowPersistence(
        workflowRepository: WorkflowRepository,
        queryableRepository: MongoFlowQueryRepository<WorkflowDocument>,
        queryMapper: QueryMapper<WorkflowData, WorkflowDocument>,
    ): WorkflowPersistence {
        return WorkflowMongoPersistence(workflowRepository, queryableRepository, queryMapper)
    }
}