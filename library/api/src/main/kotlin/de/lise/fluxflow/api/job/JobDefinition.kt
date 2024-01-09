package de.lise.fluxflow.api.job

import de.lise.fluxflow.api.job.parameter.ParameterDefinition
import de.lise.fluxflow.api.workflow.Workflow
import java.time.Instant

/**
 * The [JobDefinition] holds information on a job.
 * It mainly defines what a job is executing once the scheduled time has been reached.
 */
interface JobDefinition {
    val kind: JobKind
    val parameters: List<ParameterDefinition<*>>
    
    fun createJob(
        jobIdentifier: JobIdentifier,
        workflow: Workflow<*>,
        scheduledTime: Instant,
        cancellationKey: CancellationKey?,
        status: JobStatus
    ): Job
}