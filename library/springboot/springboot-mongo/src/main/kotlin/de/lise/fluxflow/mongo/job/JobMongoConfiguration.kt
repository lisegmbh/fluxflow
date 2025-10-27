package de.lise.fluxflow.mongo.job

import de.fluxflow.flowquery.mapper.query.QueryMapper
import de.fluxflow.flowquery.mapper.query.QueryMapperImpl
import de.lise.fluxflow.mongo.ConditionalOnFluxFlowMongo
import de.lise.fluxflow.mongo.flowquery.repository.MongoExecutor
import de.lise.fluxflow.mongo.flowquery.repository.MongoFlowQueryRepository
import de.lise.fluxflow.mongo.flowquery.repository.MongoQueryTranslator
import de.lise.fluxflow.mongo.job.flowquery.JobDataToDocumentMapper
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.job.JobPersistence
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate

@Configuration
@ConditionalOnFluxFlowMongo
open class JobMongoConfiguration {
    @Bean
    internal open fun jobMongoExecutor(mongoTemplate: MongoTemplate): MongoExecutor<JobDocument> {
        return MongoExecutor(mongoTemplate, JobDocument::class.java)
    }

    @Bean
    internal open fun jobFlowQueryRepository(
        queryTranslator: MongoQueryTranslator,
        executor: MongoExecutor<JobDocument>,
    ): MongoFlowQueryRepository<JobDocument> {
        return MongoFlowQueryRepository(
            JobDocument::class, queryTranslator, executor
        )
    }

    @Bean
    open fun jobQueryMapper(): QueryMapper<JobData, JobDocument> {
        return QueryMapperImpl(JobDataToDocumentMapper())
    }

    @Bean
    open fun jobPersistence(
        jobRepository: JobRepository,
        queryableRepository: MongoFlowQueryRepository<JobDocument>,
        queryMapper: QueryMapper<JobData, JobDocument>,
    ): JobPersistence {
        return JobMongoPersistence(jobRepository, queryableRepository, queryMapper)
    }
}