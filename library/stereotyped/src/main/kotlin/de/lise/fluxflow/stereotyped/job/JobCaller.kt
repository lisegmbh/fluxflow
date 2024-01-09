package de.lise.fluxflow.stereotyped.job

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.job.Job

fun interface JobCaller<TInstance> {
    fun call(job: Job, instance: TInstance): Continuation<*>
}