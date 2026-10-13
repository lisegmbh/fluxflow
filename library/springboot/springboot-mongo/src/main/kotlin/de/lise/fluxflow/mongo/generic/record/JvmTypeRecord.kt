package de.lise.fluxflow.mongo.generic.record

import de.lise.fluxflow.mongo.generic.TypeSpec


data class JvmTypeRecord(
    val jvmTypeReference: TypeReference,
): TypeRecord {
    override fun toTypeSpec(context: RecordContext): TypeSpec {
        return TypeRecordDecoder.toTypeSpec(this, context)
    }
}

