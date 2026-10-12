package de.lise.fluxflow.mongo

import org.springframework.data.mapping.context.MappingContext
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/** Field names from the same mapping metadata used by the application's Mongo converter. */
internal class MongoFieldNames(
    private val context: MappingContext<out MongoPersistentEntity<*>, MongoPersistentProperty>,
) {
    private data class Field(val name: String, val parts: List<String>)
    private data class Schema(
        val fields: Map<String, Field>,
        val properties: Map<List<String>, String>,
        val namespaces: Set<List<String>>,
    )

    private val schemas = ConcurrentHashMap<Class<*>, Schema>()

    private fun schema(type: Class<*>): Schema = schemas.computeIfAbsent(type) {
        val fields = linkedMapOf<String, Field>()
        val properties = linkedMapOf<List<String>, String>()
        val namespaces = linkedSetOf<List<String>>()
        context.getRequiredPersistentEntity(type).forEach { property ->
            val parts = Collections.unmodifiableList(property.mongoField.name.parts().toList())
            fields[property.name] = Field(property.fieldName, parts)
            properties.putIfAbsent(parts, property.name)
            for (length in 0 until parts.size) {
                namespaces.add(Collections.unmodifiableList(parts.take(length)))
            }
        }
        Schema(
            Collections.unmodifiableMap(fields),
            Collections.unmodifiableMap(properties),
            Collections.unmodifiableSet(namespaces),
        )
    }

    private fun field(type: Class<*>, property: String): Field =
        checkNotNull(schema(type).fields[property]) { "Required property '$property' not found for ${type.name}" }

    fun fieldName(type: Class<*>, property: String): String =
        field(type, property).name

    fun fieldParts(type: Class<*>, property: String): List<String> =
        field(type, property).parts

    fun propertyAt(type: Class<*>, path: List<String>): String? =
        schema(type).properties[path]

    fun isNamespace(type: Class<*>, path: List<String>): Boolean =
        path in schema(type).namespaces

    fun read(document: Map<*, *>, type: Class<*>, property: String): Any? =
        fieldParts(type, property).fold(document as Any?) { current, part -> (current as? Map<*, *>)?.get(part) }

    fun contains(document: Map<*, *>, type: Class<*>, property: String): Boolean {
        val parts = fieldParts(type, property)
        val parent = parts.dropLast(1).fold(document as Any?) { current, part ->
            (current as? Map<*, *>)?.get(part)
        }
        return (parent as? Map<*, *>)?.containsKey(parts.last()) == true
    }
}
