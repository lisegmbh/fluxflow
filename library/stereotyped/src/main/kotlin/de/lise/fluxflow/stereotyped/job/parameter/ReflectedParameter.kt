package de.lise.fluxflow.stereotyped.job.parameter

import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.api.job.parameter.Parameter
import de.lise.fluxflow.api.job.parameter.ParameterDefinition
import de.lise.fluxflow.stereotyped.step.data.PropertyGetter

class ReflectedParameter<TInstance, TModel>(
    override val job: Job, 
    override val definition: ParameterDefinition<TModel>,
    private val instance: TInstance,
    private val propertyAccessor: PropertyGetter<TInstance, TModel>
) : Parameter<TModel> {
    override fun get(): TModel {
        return propertyAccessor.get(instance)
    }
}