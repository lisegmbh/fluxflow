package de.lise.fluxflow.mongo.step.query

import de.lise.fluxflow.mongo.query.QueryableRepositoryImpl
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.query.filter.StepDocumentFilter
import org.springframework.data.mongodb.core.MongoTemplate

class QueryableStepRepositoryImpl(
    mongoTemplate: MongoTemplate,
) : QueryableRepositoryImpl<StepDocument, StepDocumentFilter>(
    mongoTemplate,
    StepDocument::class,
), QueryableStepRepository