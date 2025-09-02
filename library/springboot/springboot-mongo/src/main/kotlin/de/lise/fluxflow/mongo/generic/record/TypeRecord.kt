package de.lise.fluxflow.mongo.generic.record

import de.lise.fluxflow.mongo.generic.TypeSpec

interface TypeRecord {
    fun toTypeSpec(context: RecordContext): TypeSpec
}