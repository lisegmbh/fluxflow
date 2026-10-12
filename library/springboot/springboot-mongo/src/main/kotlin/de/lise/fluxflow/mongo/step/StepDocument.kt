package de.lise.fluxflow.mongo.step

import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.mongo.generic.TypeSpec
import de.lise.fluxflow.mongo.generic.ValueTypeConverter
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.generic.toGenericMap
import de.lise.fluxflow.mongo.generic.toTypeSafeData
import de.lise.fluxflow.mongo.generic.withData
import de.lise.fluxflow.persistence.step.StepData
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document


@Document
data class StepDocument(
    @Id var id: String?,
    val workflowId: String,
    val kind: String,
    val version: String?,
    @Deprecated("Use .dataEntries")
    val data: Map<String, Any?>,
    @Deprecated("Use .dataEntries")
    val dataTypeMap: Map<String, TypeSpec>,
    val dataEntries: TypedRecords<Any?>?,
    @Deprecated("Use .metadataEntries")
    val metadata: Map<String, Any>,
    @Deprecated("Use .metadataEntries")
    val metadataTypeMap: Map<String, TypeSpec>,
    val metadataEntries: TypedRecords<Any>?,
    val status: Status
) {
    /**
     * This constructor should be used when creating new object instance of [StepDocument].
     * The all-args constructor should only be used by the mongo driver.
     */
    constructor(
        id: String?,
        workflowId: String,
        kind: String,
        version: String?,
        data: Map<String, Any?>,
        metadata: Map<String, Any>,
        status: Status
    ) : this(
        id,
        workflowId,
        kind,
        version,
        data,
        data.toGenericMap().mapValues { it.value.spec },
        TypedRecords.fromData(data),
        metadata,
        metadata.toGenericMap().mapValues { it.value.spec },
        TypedRecords.fromData(metadata),
        status
    )

    private fun typeSafeData(valueTypes: ValueTypeConverter): Map<String, Any?> =
        dataEntries?.toTypeSafeData(valueTypes)
            ?: dataTypeMap.withData(data).toTypeSafeData(valueTypes)

    private fun typeSafeMetadata(valueTypes: ValueTypeConverter): Map<String, Any> =
        metadataEntries?.toTypeSafeData(valueTypes)
            ?: metadataTypeMap
                .withData(metadata)
                .toTypeSafeData(valueTypes)
                .mapValues { it.value!! }

    internal fun validateForWrite(valueTypes: ValueTypeConverter) {
        typeSafeData(valueTypes)
        typeSafeMetadata(valueTypes)
    }

    fun toStepData(): StepData = toStepData(ValueTypeConverter.builtInsOnly())

    fun toStepData(valueTypes: ValueTypeConverter): StepData {
        val typeSafeData = typeSafeData(valueTypes)
        val typeSafeMetadata = typeSafeMetadata(valueTypes)
        return StepData(
            id!!,
            workflowId,
            kind,
            version,
            typeSafeData,
            status,
            typeSafeMetadata,
        )
    }
}
