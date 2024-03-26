package de.lise.fluxflow.mongo.generic.record

import de.lise.fluxflow.mongo.generic.NullType
import de.lise.fluxflow.mongo.generic.TypeSpec

class NullTypeRecord : TypeRecord {
    override fun toTypeSpec(context: RecordContext): TypeSpec {
        return NullType()
    }
}