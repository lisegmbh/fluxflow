package de.lise.fluxflow.mongo.generic.record

import de.lise.fluxflow.mongo.generic.SimpleType
import de.lise.fluxflow.mongo.generic.TypeSpec


data class JvmTypeRecord(
    val jvmTypeReference: TypeReference,
): TypeRecord {
    override fun toTypeSpec(context: RecordContext): TypeSpec {
        return SimpleType(
            context.getType(jvmTypeReference).value
        )
    }
}

