package de.lise.fluxflow.mongo.job

import de.fluxflow.flowquery.mapper.query.QueryMapper
import de.fluxflow.flowquery.mapper.query.QueryMapperImpl
import de.lise.fluxflow.mongo.ConditionalOnFluxFlowMongo
import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.mongo.flowquery.repository.MongoExecutor
import de.lise.fluxflow.mongo.flowquery.repository.MongoFlowQueryRepository
import de.lise.fluxflow.mongo.flowquery.repository.MongoQueryTranslator
import de.lise.fluxflow.mongo.job.flowquery.JobDataToDocumentReplacer
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.job.JobPersistence
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnFluxFlowMongo
open class JobMongoConfiguration {
    @Bean
    internal open fun jobMongoExecutor(access: FluxFlowMongoAccess): MongoExecutor<JobDocument> {
        return MongoExecutor(access.template, JobDocument::class.java)
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
        return QueryMapperImpl(
            JobDataToDocumentReplacer().toMapper()
        )
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
