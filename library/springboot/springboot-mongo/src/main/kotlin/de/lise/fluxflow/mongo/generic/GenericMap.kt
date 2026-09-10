package de.lise.fluxflow.mongo.generic

typealias GenericMap = Map<String, GenericMapEntry<*>>

fun Map<String, Any?>.toGenericMap(): GenericMap {
    return this.mapValues {
        GenericMapEntry(it.value)
    }
}

fun GenericMap.toTypeSafeData(): Map<String, Any?> {
    return ValueTypeConverter.BuiltInsOnly.toTypeSafeData(
        mapValues { it.value.spec },
        mapValues { it.value.value },
    )
}

fun GenericMap.toTypeSafeData(converter: ValueTypeConverter): Map<String, Any?> {
    return converter.toTypeSafeData(
        mapValues { it.value.spec },
        mapValues { it.value.value },
    )
}

fun Map<String, TypeSpec>.withData(dataMap: Map<String, Any?>): GenericMap {
    if (keys != dataMap.keys) {
        val missingTypes = dataMap.keys - keys
        val missingValues = keys - dataMap.keys
        throw ValueTypeConversionException(
            "Value type metadata keys do not match value keys: " +
                    "missing types=$missingTypes, missing values=$missingValues."
        )
    }
    return this.mapValues {
        GenericMapEntry(
            it.value,
            dataMap[it.key]
        )
    }
}
