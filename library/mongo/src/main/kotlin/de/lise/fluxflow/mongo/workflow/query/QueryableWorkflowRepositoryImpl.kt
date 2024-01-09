package de.lise.fluxflow.mongo.workflow.query

import de.lise.fluxflow.mongo.query.QueryableRepositoryImpl
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.mongo.workflow.query.filter.WorkflowDocumentFilter
import org.springframework.data.mongodb.core.MongoTemplate

class QueryableWorkflowRepositoryImpl(
    mongoTemplate: MongoTemplate,
) : QueryableRepositoryImpl<WorkflowDocument, WorkflowDocumentFilter>(
    mongoTemplate,
    WorkflowDocument::class,
), QueryableWorkflowRepository