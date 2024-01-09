package de.lise.fluxflow.api.job.parameter

import de.lise.fluxflow.api.job.Job

interface Parameter<T> {
    val job: Job
    val definition: ParameterDefinition<T>
    
    fun get(): T
}