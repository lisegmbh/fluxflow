package de.lise.fluxflow.mongo.generic

import de.lise.fluxflow.mongo.generic.record.CollectionTypeRecord
import de.lise.fluxflow.mongo.generic.record.RecordContext
import de.lise.fluxflow.mongo.generic.record.TypeRecord

class CollectionType(
    val collectionType: TypeSpec,
    val componentTypes: List<TypeSpec>
) : TypeSpec {
    override fun assertType(value: Any?): Any? {
        val elements = (value as Collection<*>).mapIndexed { index, element ->
            componentTypes[index].assertType(element)
        }
        return collectionType.assertType(elements)
    }

    override fun toRecord(context: RecordContext): TypeRecord {
        return CollectionTypeRecord(
            collectionType.toRecord(context),
            componentTypes.map { it.toRecord(context) }
        )
    }
}