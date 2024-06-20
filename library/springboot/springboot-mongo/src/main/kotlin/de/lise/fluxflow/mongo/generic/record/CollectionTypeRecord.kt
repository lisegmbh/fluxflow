package de.lise.fluxflow.mongo.generic.record

import de.lise.fluxflow.mongo.generic.CollectionType
import de.lise.fluxflow.mongo.generic.TypeSpec

data class CollectionTypeRecord(
    val collectionType: TypeRecord,
    val componentTypes: List<TypeRecord>
) : TypeRecord {
    override fun toTypeSpec(context: RecordContext): TypeSpec {
        return CollectionType(
            collectionType.toTypeSpec(context),
            componentTypes.map {
                it.toTypeSpec(context)
            }
        )
    }
}