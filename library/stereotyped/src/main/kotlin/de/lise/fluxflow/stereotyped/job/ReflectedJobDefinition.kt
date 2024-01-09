package de.lise.fluxflow.stereotyped.job

import de.lise.fluxflow.api.job.*
import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.api.job.parameter.Parameter
import de.lise.fluxflow.api.job.parameter.ParameterDefinition
import de.lise.fluxflow.api.workflow.Workflow
import java.time.Instant

class ReflectedJobDefinition<TInstance>(
    override val kind: JobKind,
    override val parameters: List<ParameterDefinition<*>>,
    private val instance: TInstance,
    private val jobCaller: JobCaller<TInstance>
) : JobDefinition {
    override fun createJob(
        jobIdentifier: JobIdentifier,
        workflow: Workflow<*>,
        scheduledTime: Instant,
        cancellationKey: CancellationKey?,
        status: JobStatus
    ): Job {
        /* 
            We need to use a mutable list with deferred initialization,
            because of the circular dependency between ReflectedJob and its parameters.
         */
        val parameterList = mutableListOf<Parameter<*>>()

        val job = ReflectedJob(
            jobIdentifier,
            this,
            scheduledTime,
            cancellationKey,
            status,
            workflow,
            parameterList,
            instance,
            jobCaller
        )
        
        parameterList.addAll(0, parameters.map { 
            it.createParameter(job)
        })
        
        return job
    }
}