package de.lise.fluxflow.mongo.job.query

import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.job.JobDocumentFilter
import de.lise.fluxflow.mongo.job.query.QueryableJobRepository
import de.lise.fluxflow.mongo.query.QueryableRepositoryImpl
import org.springframework.data.mongodb.core.MongoTemplate

class QueryableJobRepositoryImpl(
    mongoTemplate: MongoTemplate,
) : QueryableRepositoryImpl<JobDocument, JobDocumentFilter>(
    mongoTemplate,
    JobDocument::class,
), QueryableJobRepository