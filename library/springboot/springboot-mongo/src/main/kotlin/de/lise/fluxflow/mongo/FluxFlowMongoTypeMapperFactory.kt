package de.lise.fluxflow.mongo

import org.springframework.data.mongodb.core.convert.MongoTypeMapper

/** Spring-Data-version-specific bridge for the TypeInformation package change. */
internal fun interface FluxFlowMongoTypeMapperFactory {
    fun create(aliases: FluxFlowMongoTypeAliases, typeKey: String): MongoTypeMapper
}
