package de.lise.fluxflow.stereotyped.step.data

fun interface PropertySetter<TInstance, TModel> {
    fun set(instance: TInstance, newValue: TModel)
}