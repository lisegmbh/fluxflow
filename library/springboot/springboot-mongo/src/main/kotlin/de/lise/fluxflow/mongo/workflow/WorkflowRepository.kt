package de.lise.fluxflow.mongo.workflow

import de.lise.fluxflow.mongo.workflow.query.QueryableWorkflowRepository
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.NoRepositoryBean


@NoRepositoryBean
interface WorkflowRepository : MongoRepository<WorkflowDocument, String>, QueryableWorkflowRepository

