package de.lise.fluxflow.mongo.generic.record

import java.util.*

data class JvmTypeMapping(
    val entries: MutableList<TypeRecordEntry> = mutableListOf()
) : RecordContext {
    override fun registerType(typeName: TypeName): TypeReference {
        synchronized(entries) {
            val existing = entries.firstOrNull { it.type == typeName.value }
            if (existing != null) {
                return TypeReference(existing.reference)
            }

            var reference: String
            do {
                reference = UUID.randomUUID().toString()
            } while (entries.any { it.reference == reference })

            entries.add(
                TypeRecordEntry(
                    typeName.value,
                    reference
                )
            )
            return TypeReference(reference)
        }
    }

    override fun getType(reference: TypeReference): TypeName {
        return entries.first {
            it.reference == reference.value
        }.let {
            TypeName(it.type)
        }
    }
}