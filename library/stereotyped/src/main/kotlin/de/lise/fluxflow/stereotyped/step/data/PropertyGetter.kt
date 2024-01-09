package de.lise.fluxflow.stereotyped.step.data

fun interface PropertyGetter<TInstance, TModel> {
    fun get(instance: TInstance): TModel
}