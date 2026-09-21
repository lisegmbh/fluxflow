package de.lise.fluxflow.mongo.generic

import de.lise.fluxflow.mongo.generic.record.JvmTypeRecord
import de.lise.fluxflow.mongo.generic.record.RecordContext
import de.lise.fluxflow.mongo.generic.record.TypeName
import de.lise.fluxflow.mongo.generic.record.TypeRecord
import kotlin.reflect.KClass

class SimpleType(val typeName: String) : TypeSpec {
    constructor(type: KClass<*>) : this(type.java.canonicalName)

    override fun assertType(value: Any?): Any? {
        return ValueTypeConverter.BuiltInsOnly.assertType(this, value)
    }

    fun assertType(value: Any?, converter: ValueTypeConverter): Any? =
        converter.assertType(this, value)

    override fun toRecord(context: RecordContext): TypeRecord {
        return JvmTypeRecord(
            context.registerType(TypeName(typeName))
        )
    }
}
