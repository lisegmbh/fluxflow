package de.lise.fluxflow.engine.step.definition

import de.lise.fluxflow.api.step.StepDefinition
import de.lise.fluxflow.api.step.stateful.StatefulStepDefinition
import de.lise.fluxflow.persistence.step.definition.DataDefinitionData
import de.lise.fluxflow.persistence.step.definition.StepDefinitionData
import de.lise.fluxflow.persistence.step.definition.StepDefinitionPersistence

class StepDefinitionService(
    private val stepDefinitionPersistence: StepDefinitionPersistence
) {
    fun save(
        stepDefinition: StepDefinition
    ) {
        stepDefinitionPersistence.save(StepDefinitionData(
            stepDefinition.kind.value,
            stepDefinition.version.version,
            stepDefinition.metadata,
            (stepDefinition as? StatefulStepDefinition)?.data?.map { 
                DataDefinitionData(
                    it.kind.value,
                    it.type.typeName,
                    it.metadata,
                    it.isCalculatedValue
                )
            } ?: emptyList()
        ))
    }
}