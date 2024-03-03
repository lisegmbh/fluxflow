package de.lise.fluxflow.mongo.query

import de.lise.fluxflow.reflection.property.findAnnotationEverywhere
import org.springframework.data.mongodb.core.mapping.Field
import kotlin.reflect.KProperty1

fun KProperty1<*, *>.getMongoFieldName(): String {
    val nameFromAnnotation = this.findAnnotationEverywhere<Field>()
        ?.let {
            it.name.ifEmpty { it.value }
        }?.ifEmpty { null }

    if (nameFromAnnotation != null) {
        return nameFromAnnotation
    }

    // this is a workaround for the fact that the mongo does not support the name "id" for fields
    if (this.name == "id") {
        return "_id"
    }

    return this.name
}