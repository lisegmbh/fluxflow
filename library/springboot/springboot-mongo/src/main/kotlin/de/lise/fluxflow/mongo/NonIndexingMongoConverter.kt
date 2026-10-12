package de.lise.fluxflow.mongo

import org.springframework.data.mapping.context.MappingContext
import org.springframework.data.mongodb.core.convert.MongoConverter
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty

/**
 * Gives the internal template the host mapping metadata without letting template initialization
 * install an index creator or replace the shared context's application event publisher.
 */
internal class NonIndexingMongoConverter(delegate: MongoConverter) : MongoConverter by delegate {
    private val nonIndexingContext = NonIndexingMappingContext(delegate.mappingContext)

    override fun getMappingContext(): MappingContext<out MongoPersistentEntity<*>, MongoPersistentProperty> =
        nonIndexingContext
}

/** Deliberately implements neither MongoMappingContext nor ApplicationEventPublisherAware. */
private class NonIndexingMappingContext(
    delegate: MappingContext<out MongoPersistentEntity<*>, MongoPersistentProperty>,
) : MappingContext<MongoPersistentEntity<*>, MongoPersistentProperty> by delegate.asMongoMappingContext()

@Suppress("UNCHECKED_CAST")
private fun MappingContext<out MongoPersistentEntity<*>, MongoPersistentProperty>.asMongoMappingContext():
    MappingContext<MongoPersistentEntity<*>, MongoPersistentProperty> =
    this as MappingContext<MongoPersistentEntity<*>, MongoPersistentProperty>
