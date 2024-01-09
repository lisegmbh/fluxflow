package de.lise.fluxflow.api.job.parameter

import de.lise.fluxflow.api.job.Job

interface ParameterDefinition<T> {
    val kind: ParameterKind
    val type: Class<out T>
    
    fun createParameter(job: Job): Parameter<T>
}