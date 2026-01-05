package de.lise.fluxflow.mongo.continuation.history

import de.fluxflow.flowquery.mapper.query.QueryMapper
import de.fluxflow.flowquery.mapper.query.QueryMapperImpl
import de.lise.fluxflow.mongo.ConditionalOnFluxFlowMongo
import de.lise.fluxflow.mongo.continuation.history.flowquery.ContinuationRecordDataToDocumentReplacer
import de.lise.fluxflow.mongo.flowquery.repository.MongoExecutor
import de.lise.fluxflow.mongo.flowquery.repository.MongoFlowQueryRepository
import de.lise.fluxflow.mongo.flowquery.repository.MongoQueryTranslator
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordData
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordPersistence
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate

@Configuration
@ConditionalOnFluxFlowMongo
open class ContinuationMongoConfiguration {

    @Bean
    internal open fun continuationMongoExecutor(mongoTemplate: MongoTemplate): MongoExecutor<ContinuationRecordDocument> {
        return MongoExecutor(mongoTemplate, ContinuationRecordDocument::class.java)
    }

    @Bean
    internal open fun continuationFlowQueryRepository(
        queryTranslator: MongoQueryTranslator,
        executor: MongoExecutor<ContinuationRecordDocument>,
    ): MongoFlowQueryRepository<ContinuationRecordDocument> {
        return MongoFlowQueryRepository(
            ContinuationRecordDocument::class, queryTranslator, executor
        )
    }

    @Bean
    open fun continuationQueryMapper(): QueryMapper<ContinuationRecordData, ContinuationRecordDocument> {
        return QueryMapperImpl(
            ContinuationRecordDataToDocumentReplacer().toMapper()
        )
    }

    @Bean
    open fun continuationRecordPersistence(
        continuationRecordRepository: ContinuationRecordRepository,
        queryableRepository: MongoFlowQueryRepository<ContinuationRecordDocument>,
        queryMapper: QueryMapper<ContinuationRecordData, ContinuationRecordDocument>,
    ): ContinuationRecordPersistence =
        ContinuationRecordMongoPersistence(continuationRecordRepository, queryableRepository, queryMapper)
}