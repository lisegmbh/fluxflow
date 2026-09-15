package de.lise.fluxflow.mongo.step

import de.fluxflow.flowquery.mapper.query.QueryMapper
import de.fluxflow.flowquery.mapper.query.QueryMapperImpl
import de.lise.fluxflow.mongo.ConditionalOnFluxFlowMongo
import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.mongo.flowquery.repository.MongoExecutor
import de.lise.fluxflow.mongo.flowquery.repository.MongoFlowQueryRepository
import de.lise.fluxflow.mongo.flowquery.repository.MongoQueryTranslator
import de.lise.fluxflow.mongo.step.flowquery.StepDataToDocumentReplacer
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.StepPersistence
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnFluxFlowMongo
open class StepMongoConfiguration {
    @Bean
    internal open fun stepMongoExecutor(access: FluxFlowMongoAccess): MongoExecutor<StepDocument> {
        return MongoExecutor(access.template, StepDocument::class.java)
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
        return QueryMapperImpl(
            StepDataToDocumentReplacer().toMapper()
        )
    }

    @Bean
    open fun stepPersistence(
        stepRepository: StepRepository,
        queryableRepository: MongoFlowQueryRepository<StepDocument>,
        queryMapper: QueryMapper<StepData, StepDocument>,
    ): StepPersistence =
        StepMongoPersistence(stepRepository, queryableRepository, queryMapper)
}
