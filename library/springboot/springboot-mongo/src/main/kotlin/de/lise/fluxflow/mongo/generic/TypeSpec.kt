package de.lise.fluxflow.mongo.generic

import de.lise.fluxflow.mongo.generic.record.RecordContext
import de.lise.fluxflow.mongo.generic.record.TypeRecord

interface TypeSpec {
    fun assertType(value: Any?): Any?
    fun toRecord(context: RecordContext): TypeRecord
    companion object {
        fun fromValue(value: Any?): TypeSpec {
            if (value == null) {
                return NullType()
            }

            if (value !is Map<*,*> && value is Collection<*>) {
                return CollectionType(
                    SimpleType(value::class),
                    value.map { fromValue(it) }
                )
            }

            return SimpleType(value::class.java.canonicalName)
        }
    }
}