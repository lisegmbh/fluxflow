package de.lise.fluxflow.mongo.generic.record

import de.lise.fluxflow.mongo.generic.TypeSpec
import de.lise.fluxflow.mongo.generic.toTypeSafeData
import de.lise.fluxflow.mongo.generic.withData

data class TypedRecords<TValue>(
    val jvmTypes: JvmTypeMapping,
    val values: Map<String, TValue>,
    val types: Map<String, TypeRecord>
) {
    fun toTypeSafeData(): Map<String, TValue> {
        return types.mapValues { it.value.toTypeSpec(jvmTypes) }
            .withData(values)
            .toTypeSafeData()
            .mapValues {
                @Suppress("UNCHECKED_CAST")
                it.value as TValue
            }
    }

    companion object {
        fun <TValue> fromData(data: Map<String, TValue>): TypedRecords<TValue> {
            val context = JvmTypeMapping()
            val types = data.mapValues {
                TypeSpec.fromValue(it.value).toRecord(context)
            }
            return TypedRecords(
                context,
                data,
                types
            )
        }
    }
}