package de.lise.fluxflow.mongo.step.definition

import de.lise.fluxflow.persistence.step.definition.StepDefinitionData
import de.lise.fluxflow.persistence.step.definition.StepDefinitionPersistence
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.*


class StepDefinitionMongoPersistence(
    private val repository: StepDefinitionRepository,
    private val template: MongoTemplate
) : StepDefinitionPersistence {
    override fun save(stepDefinition: StepDefinitionData): StepDefinitionData {
        val document = StepDefinitionDocument(stepDefinition)
        val query = Query(
            where(StepDefinitionDocument::kind).isEqualTo(stepDefinition.kind)
                .and(StepDefinitionDocument::version).isEqualTo(stepDefinition.version)
        )
        val update = Update()
            .setOnInsert(StepDefinitionDocument::kind.name, document.kind)
            .setOnInsert(StepDefinitionDocument::version.name, document.version)
            .set(StepDefinitionDocument::metadata.name, document.metadata)
            .set(StepDefinitionDocument::data.name, document.data)
        
        template.upsert(
            query,
            update,
            StepDefinitionDocument::class.java
        )
        
        return document.toStepDefinitionData()
    }

    override fun findForKindAndVersion(stepKind: String, version: String): StepDefinitionData? {
        return repository.findByKindAndVersion(
            stepKind,
            version
        )?.toStepDefinitionData()
    }
}