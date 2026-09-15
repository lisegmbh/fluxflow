package de.lise.fluxflow.mongo.generic

import de.lise.fluxflow.mongo.generic.record.NullTypeRecord
import de.lise.fluxflow.mongo.generic.record.RecordContext
import de.lise.fluxflow.mongo.generic.record.TypeRecord

class NullType : TypeSpec {
    override fun assertType(value: Any?): Any? {
        return ValueTypeConverter.BuiltInsOnly.assertType(this, value)
    }

    fun assertType(value: Any?, converter: ValueTypeConverter): Any? =
        converter.assertType(this, value)

    override fun toRecord(context: RecordContext): TypeRecord {
        return NullTypeRecord()
    }
}
