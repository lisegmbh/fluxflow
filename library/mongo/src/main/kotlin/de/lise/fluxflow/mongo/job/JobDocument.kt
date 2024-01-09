package de.lise.fluxflow.mongo.job

import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.mongo.generic.TypeSpec
import de.lise.fluxflow.mongo.generic.toGenericMap
import de.lise.fluxflow.mongo.generic.toTypeSafeData
import de.lise.fluxflow.mongo.generic.withData
import de.lise.fluxflow.persistence.job.JobData
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant


@Document
data class JobDocument(
    @Id var id: String?,
    val workflowId: String,
    val kind: String,
    val parameters: Map<String, Any?>,
    val parameterTypeMap: Map<String, TypeSpec>,
    val scheduledTime: Instant,
    val cancellationKey: String?,
    val jobStatus: JobStatus
) {
    
    constructor(
        id: String?,
        workflowId: String,
        kind: String,
        data: Map<String, Any?>,
        scheduledTime: Instant,
        cancellationKey: String?,
        jobStatus: JobStatus
    ): this(
        id,
        workflowId,
        kind,
        data,
        data.toGenericMap().mapValues { it.value.spec },
        scheduledTime,
        cancellationKey,
        jobStatus
    )
    
    private val typeSafeData: Map<String, Any?>
        get() =  parameterTypeMap.withData(parameters).toTypeSafeData()
    
    fun toJobData(): JobData {
        return JobData(
            id!!,
            workflowId,
            kind,
            typeSafeData,
            scheduledTime,
            cancellationKey,
            jobStatus
        )
    }
}