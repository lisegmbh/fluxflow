package de.lise.fluxflow.mongo.generic

import de.lise.fluxflow.mongo.generic.record.JvmTypeRecord
import de.lise.fluxflow.mongo.generic.record.RecordContext
import de.lise.fluxflow.mongo.generic.record.TypeName
import de.lise.fluxflow.mongo.generic.record.TypeRecord
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class SimpleType(val typeName: String) : TypeSpec {
    constructor(type: KClass<*>) : this(type.java.canonicalName)

    override fun assertType(value: Any?): Any? {
        val type = try {
             Thread.currentThread().contextClassLoader.loadClass(typeName).kotlin
        } catch (e: ClassNotFoundException) {
            Logger.warn("Could not load type {}. We will try to use the value anyway.", typeName, e)
            return value
        }

        if(type.isInstance(value)) {
            return value
        }

        if(value is Collection<*>) {
            if(type.isSubclassOf(Set::class)) {
                return value.toSet()
            }
            if(type.isSubclassOf(List::class)) {
                return value.toList()
            }
        }

        if(value is Date && type.isSubclassOf(Instant::class)) {
            return value.toInstant()
        }

        if(value is String && type.isSubclassOf(Enum::class)) {
            @Suppress("UNCHECKED_CAST")
            return (type.java.enumConstants as Array<Enum<*>>).first {
                it.name.equals(value, true)
            }
        }

        return value
    }

    override fun toRecord(context: RecordContext): TypeRecord {
        return JvmTypeRecord(
            context.registerType(TypeName(typeName))
        )
    }

    private companion object {
        val Logger = LoggerFactory.getLogger(SimpleType::class.java)!!
    }
}