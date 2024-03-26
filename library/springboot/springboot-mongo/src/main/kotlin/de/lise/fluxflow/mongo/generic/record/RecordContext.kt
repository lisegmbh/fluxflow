package de.lise.fluxflow.mongo.generic.record

interface RecordContext {
    fun registerType(typeName: TypeName): TypeReference
    fun getType(reference: TypeReference): TypeName
}