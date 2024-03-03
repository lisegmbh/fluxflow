package de.lise.fluxflow.mongo.generic

typealias GenericMap = Map<String, GenericMapEntry<*>>

fun Map<String, Any?>.toGenericMap(): GenericMap {
    return this.mapValues {
        GenericMapEntry(it.value)
    }
}

fun GenericMap.toTypeSafeData(): Map<String, Any?> {
    return this.mapValues {
        it.value.spec.assertType(it.value.value)
    }
}

fun Map<String, TypeSpec>.withData(dataMap: Map<String, Any?>): GenericMap {
    return this.mapValues {
        GenericMapEntry(
            it.value,
            dataMap[it.key]
        )
    }
}