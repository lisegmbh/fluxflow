package de.lise.fluxflow.mongo.bootstrapping.collation

import org.springframework.data.mongodb.core.query.Collation

/**
 * The [CollationConfigurer] is used to obtain the default collation to be used
 * when initially creating a MongoDB collection for a specified document.
 */
interface CollationConfigurer {
    /**
     * Is called to obtain the default collation settings for a specified document [type].
     * @param type The document type indicating the collection that is about to be created.
     * Implementations may use this as a discriminator to return collection-specific collation settings.
     * @return The collation settings to be used when initially creating the collection.
     * May return `null`, if MongoDB's default should be used.
     */
    fun <TDocument : Any> configureCollation(type: Class<TDocument>): Collation?
}