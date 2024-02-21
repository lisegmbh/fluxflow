package de.lise.fluxflow.stereotyped.job

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.job.*
import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.api.job.parameter.Parameter
import de.lise.fluxflow.api.workflow.Workflow
import java.time.Instant

class ReflectedJob<TInstance>(
    override val identifier: JobIdentifier,
    override val definition: JobDefinition,
    override val scheduledTime: Instant,
    override val cancellationKey: CancellationKey?,
    override val status: JobStatus,
    override val workflow: Workflow<*>,
    override val parameters: List<Parameter<*>>,
    override val metadata: Map<String, Any>,
    private val instance: TInstance,
    private val jobCaller: JobCaller<TInstance>
) : Job {
    override fun execute(): Continuation<*> {
        return jobCaller.call(this, instance)
    }
}