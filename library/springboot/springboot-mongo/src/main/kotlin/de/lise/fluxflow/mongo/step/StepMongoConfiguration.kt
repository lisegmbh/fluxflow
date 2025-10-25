package de.lise.fluxflow.mongo.step

import de.fluxflow.flowquery.mapper.query.QueryMapper
import de.fluxflow.flowquery.mapper.query.QueryMapperImpl
import de.lise.fluxflow.mongo.ConditionalOnFluxFlowMongo
import de.lise.fluxflow.mongo.flowquery.repository.MongoExecutor
import de.lise.fluxflow.mongo.flowquery.repository.MongoFlowQueryRepository
import de.lise.fluxflow.mongo.flowquery.repository.MongoQueryTranslator
import de.lise.fluxflow.mongo.step.flowquery.StepDataToDocumentMapper
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.StepPersistence
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate

@Configuration
@ConditionalOnFluxFlowMongo
open class StepMongoConfiguration {
    @Bean
    internal open fun stepMongoExecutor(mongoTemplate: MongoTemplate): MongoExecutor<StepDocument> {
        return MongoExecutor(mongoTemplate, StepDocument::class.java)
    }

    @Bean
    internal open fun stepFlowQueryRepository(
        queryTranslator: MongoQueryTranslator,
        executor: MongoExecutor<StepDocument>,
    ): MongoFlowQueryRepository<StepDocument> {
        return MongoFlowQueryRepository(
            StepDocument::class, queryTranslator, executor
        )
    }

    @Bean
    open fun stepDocumentMapper(): QueryMapper<StepData, StepDocument> {
        return QueryMapperImpl(StepDataToDocumentMapper())
    }

    @Bean
    open fun stepPersistence(
        stepRepository: StepRepository,
        queryableRepository: MongoFlowQueryRepository<StepDocument>,
        queryMapper: QueryMapper<StepData, StepDocument>,
    ): StepPersistence =
        StepMongoPersistence(stepRepository, queryableRepository, queryMapper)
}