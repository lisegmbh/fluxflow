package de.lise.fluxflow.mongo.generic.record

import java.util.*

data class JvmTypeMapping(
    val entries: MutableMap<String, String> = emptyMap<String, String>().toMutableMap()
) : RecordContext {
    override fun registerType(typeName: TypeName): TypeReference {
        return entries.computeIfAbsent(typeName.value) {
            synchronized(entries) {
                var reference: String
                do {
                    reference = UUID.randomUUID().toString()
                } while (entries.any { it.value == reference })
                reference
            }
        }.let { TypeReference(it) }
    }

    override fun getType(reference: TypeReference): TypeName {
        return entries.entries.first {
            it.value == reference.value
        }.let { TypeName(it.key) }
    }
}