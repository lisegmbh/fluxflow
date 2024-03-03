package de.lise.fluxflow.mongo.workflow.query

import de.lise.fluxflow.mongo.query.QueryableRepository
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.mongo.workflow.query.filter.WorkflowDocumentFilter

interface QueryableWorkflowRepository : QueryableRepository<WorkflowDocument, WorkflowDocumentFilter>