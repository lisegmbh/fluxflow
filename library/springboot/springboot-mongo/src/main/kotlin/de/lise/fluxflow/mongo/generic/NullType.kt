package de.lise.fluxflow.mongo.generic

import de.lise.fluxflow.mongo.generic.record.NullTypeRecord
import de.lise.fluxflow.mongo.generic.record.RecordContext
import de.lise.fluxflow.mongo.generic.record.TypeRecord

class NullType : TypeSpec {
    override fun assertType(value: Any?): Any? {
        return null // A null type always represents a null value
    }

    override fun toRecord(context: RecordContext): TypeRecord {
        return NullTypeRecord()
    }
}