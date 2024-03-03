package de.lise.fluxflow.mongo.step

import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.mongo.generic.TypeSpec
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
    val data: Map<String, Any?>,
    val dataTypeMap: Map<String, TypeSpec>,
    val metadata: Map<String, Any>,
    val metadataTypeMap: Map<String, TypeSpec>,
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
        data: Map<String, Any?>,
        metadata: Map<String, Any>,
        status: Status
    ): this(
        id,
        workflowId,
        kind,
        data,
        data.toGenericMap().mapValues { it.value.spec },
        metadata,
        metadata.toGenericMap().mapValues { it.value.spec },
        status
    )

    private val typeSafeData: Map<String, Any?>
        get() = dataTypeMap.withData(data).toTypeSafeData()

    private val typeSafeMetadata: Map<String, Any>
        get() = metadataTypeMap.withData(metadata).toTypeSafeData()
            .filterValues { it != null }
            .mapValues { it.value!! }

    fun toStepData(): StepData {
        return StepData(
            id!!,
            workflowId,
            kind,
            typeSafeData,
            status,
            typeSafeMetadata
        )
    }
}
