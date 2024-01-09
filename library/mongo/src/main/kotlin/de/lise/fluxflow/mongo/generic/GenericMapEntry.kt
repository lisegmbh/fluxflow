package de.lise.fluxflow.mongo.generic

data class GenericMapEntry<out TModel>(
    val spec: TypeSpec,
    val value: TModel
) {

    constructor(value: TModel) : this(
        TypeSpec.fromValue(value),
        value
    )
}