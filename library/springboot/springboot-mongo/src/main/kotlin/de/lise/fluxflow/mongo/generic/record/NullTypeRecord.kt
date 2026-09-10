package de.lise.fluxflow.mongo.generic.record

import de.lise.fluxflow.mongo.generic.TypeSpec

class NullTypeRecord : TypeRecord {
    override fun toTypeSpec(context: RecordContext): TypeSpec {
        return TypeRecordDecoder.toTypeSpec(this, context)
    }
}
