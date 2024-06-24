package de.lise.fluxflow.mongo.step.definition

import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.persistence.step.definition.StepDefinitionData
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class StepDefinitionDocument(
    @Id var id: String?,
    val kind: String,
    val version: String,
    val metadata: TypedRecords<Any>,
    val data: List<DataDefinitionDocument>
) {
    constructor(
        stepDefinitionData: StepDefinitionData
    ) : this(
        null,
        stepDefinitionData.kind,
        stepDefinitionData.version,
        TypedRecords.fromData(stepDefinitionData.metadata),
        stepDefinitionData.data.map { DataDefinitionDocument(it) }
    )
    
    fun toStepDefinitionData(): StepDefinitionData {
        return StepDefinitionData(
            kind,
            version,
            metadata.toTypeSafeData(),
            data.map { it.toDataDefinitionData() }
        )
    }
}