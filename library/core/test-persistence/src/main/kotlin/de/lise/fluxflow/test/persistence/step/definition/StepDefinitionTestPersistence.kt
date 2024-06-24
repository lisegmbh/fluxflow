package de.lise.fluxflow.test.persistence.step.definition

import de.lise.fluxflow.persistence.step.definition.StepDefinitionData
import de.lise.fluxflow.persistence.step.definition.StepDefinitionPersistence

class StepDefinitionTestPersistence(
    private val entities: MutableMap<Pair<String, String>, StepDefinitionData> = mutableMapOf()
) : StepDefinitionPersistence {

    override fun save(stepDefinition: StepDefinitionData): StepDefinitionData {
        entities[Pair(stepDefinition.kind, stepDefinition.version)] = stepDefinition
        return stepDefinition
    }

    override fun findForKindAndVersion(stepKind: String, version: String): StepDefinitionData? {
        return entities[Pair(stepKind, version)]
    }
}