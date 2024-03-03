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

    /**
     * A simple key-value map providing additional metadata about this
     * job definition and jobs that are going to be produced by it.
     */
    val metadata: Map<String, Any>

    fun createJob(
        jobIdentifier: JobIdentifier,
        workflow: Workflow<*>,
        scheduledTime: Instant,
        cancellationKey: CancellationKey?,
        status: JobStatus
    ): Job
}