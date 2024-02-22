package de.lise.fluxflow.api.job

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.job.parameter.Parameter
import de.lise.fluxflow.api.workflow.Workflow
import java.time.Instant

interface Job {
    val identifier: JobIdentifier
    val definition: JobDefinition
    val scheduledTime: Instant
    val cancellationKey: CancellationKey?
    val status: JobStatus
    val workflow: Workflow<*>
    val parameters: List<Parameter<*>>

    /**
     * Arbitrary metainformation associated with this job.
     */
    val metadata: Map<String, Any>
    fun execute(): Continuation<*>
}