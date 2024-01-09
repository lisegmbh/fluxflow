package de.lise.fluxflow.stereotyped.job.parameter

import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.api.job.parameter.Parameter
import de.lise.fluxflow.api.job.parameter.ParameterDefinition
import de.lise.fluxflow.api.job.parameter.ParameterKind
import de.lise.fluxflow.stereotyped.step.data.PropertyGetter

class ReflectedParameterDefinition<TInstance, TModel>(
    override val kind: ParameterKind,
    override val type: Class<out TModel>,
    private val instance: TInstance,
    private val propertyGetter: PropertyGetter<TInstance, TModel>
) : ParameterDefinition<TModel> {
    
    override fun createParameter(job: Job): Parameter<TModel> {
        return ReflectedParameter(
            job,
            this,
            instance,
            propertyGetter
        )
    }
}