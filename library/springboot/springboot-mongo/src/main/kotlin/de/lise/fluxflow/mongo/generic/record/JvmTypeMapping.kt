package de.lise.fluxflow.mongo.generic.record

import de.lise.fluxflow.mongo.generic.ValueTypeConversionException
import java.util.*

data class JvmTypeMapping(
    val entries: MutableList<TypeRecordEntry> = mutableListOf()
) : RecordContext {
    internal fun snapshot(): RecordContext = synchronized(entries) {
        JvmTypeMapping(entries.toMutableList())
    }

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
        val matches = synchronized(entries) {
            entries.filter { it.reference == reference.value }
        }
        if (matches.isEmpty()) {
            throw ValueTypeConversionException(
                "No JVM type mapping exists for reference '${reference.value}'."
            )
        }
        if (matches.size > 1) {
            throw ValueTypeConversionException(
                "JVM type reference '${reference.value}' is registered more than once."
            )
        }
        val type = matches.single().type
        if (type.isBlank()) {
            throw ValueTypeConversionException(
                "JVM type reference '${reference.value}' has an empty type name."
            )
        }
        return TypeName(type)
    }
}
