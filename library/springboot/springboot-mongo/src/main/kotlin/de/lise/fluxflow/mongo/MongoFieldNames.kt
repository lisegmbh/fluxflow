package de.lise.fluxflow.mongo

import org.springframework.data.mapping.context.MappingContext
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty

/** Field names from the same mapping metadata used by the application's Mongo converter. */
internal class MongoFieldNames(
    private val context: MappingContext<out MongoPersistentEntity<*>, MongoPersistentProperty>,
) {
    fun fieldName(type: Class<*>, property: String): String =
        context.getRequiredPersistentEntity(type).getRequiredPersistentProperty(property).fieldName

    fun fieldParts(type: Class<*>, property: String): List<String> =
        context.getRequiredPersistentEntity(type).getRequiredPersistentProperty(property).mongoField.name.parts().toList()

    fun propertyAt(type: Class<*>, path: List<String>): String? =
        context.getRequiredPersistentEntity(type).firstOrNull { it.mongoField.name.parts().toList() == path }?.name

    fun isNamespace(type: Class<*>, path: List<String>): Boolean =
        context.getRequiredPersistentEntity(type).any {
            val parts = it.mongoField.name.parts().toList()
            parts.size > path.size && parts.take(path.size) == path
        }

    fun read(document: Map<*, *>, type: Class<*>, property: String): Any? =
        fieldParts(type, property).fold(document as Any?) { current, part -> (current as? Map<*, *>)?.get(part) }
}
