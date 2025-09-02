package de.lise.fluxflow.mongo.step.definition

import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.persistence.step.definition.DataDefinitionData

data class DataDefinitionDocument(
    val kind: String,
    val type: String,
    val metadata: TypedRecords<Any>,
    val isCalculatedValue: Boolean
) {
    constructor(dataDefinitionData: DataDefinitionData) : this(
        dataDefinitionData.kind,
        dataDefinitionData.type,
        TypedRecords.fromData(dataDefinitionData.metadata),
        dataDefinitionData.isCalculatedValue
    )

    fun toDataDefinitionData(): DataDefinitionData {
        return DataDefinitionData(
            kind,
            type,
            metadata.toTypeSafeData(),
            isCalculatedValue
        )
    }
}