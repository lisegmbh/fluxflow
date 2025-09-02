package de.lise.fluxflow.mongo.step.definition

import org.springframework.data.mongodb.repository.MongoRepository

interface StepDefinitionRepository : MongoRepository<StepDefinitionDocument, String> {
    fun findByKindAndVersion(kind: String, version: String): StepDefinitionDocument?
}