package de.lise.fluxflow.mongo.generic

import de.lise.fluxflow.mongo.generic.record.CollectionTypeRecord
import de.lise.fluxflow.mongo.generic.record.RecordContext
import de.lise.fluxflow.mongo.generic.record.TypeRecord

class CollectionType(
    val collectionType: TypeSpec,
    val componentTypes: List<TypeSpec>
) : TypeSpec {
    override fun assertType(value: Any?): Any? {
        return ValueTypeConverter.BuiltInsOnly.assertType(this, value)
    }

    fun assertType(value: Any?, converter: ValueTypeConverter): Any? =
        converter.assertType(this, value)

    override fun toRecord(context: RecordContext): TypeRecord {
        return CollectionTypeRecord(
            collectionType.toRecord(context),
            componentTypes.map { it.toRecord(context) }
        )
    }
}
