package de.lise.fluxflow.mongo.generic.record

import de.lise.fluxflow.mongo.generic.TypeSpec

data class CollectionTypeRecord(
    val collectionType: TypeRecord,
    val componentTypes: List<TypeRecord>
) : TypeRecord {
    override fun toTypeSpec(context: RecordContext): TypeSpec {
        return TypeRecordDecoder.toTypeSpec(this, context)
    }
}
