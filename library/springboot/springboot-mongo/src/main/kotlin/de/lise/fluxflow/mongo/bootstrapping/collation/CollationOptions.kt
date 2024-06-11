package de.lise.fluxflow.mongo.bootstrapping.collation

import org.springframework.data.mongodb.core.query.Collation

/**
 * The configuration options used to customize a MongoDB collation.
 * See [Mongo DB's Collation Document](https://www.mongodb.com/docs/manual/reference/collation/#collation-document) for more information.
 */
data class CollationOptions(
    val locale: String,
    val strength: Int?,
    val caseLevel: Boolean?,
    val caseFirst: String?,
    val numericOrdering: Boolean?,
    val alternate: String?,
    val maxVariable: String?,
    val backwards: Boolean?,
    val normalization: Boolean?
) {
    /**
     * Creates a [Collation] object based on the options present on this instance.
     */
    fun build(): Collation {
        var collation = Collation.of(locale)

        strength?.let { collation = collation.strength(it) }
        caseLevel?.let { collation = collation.caseLevel(it) }
        caseFirst?.let { collation = collation.caseFirst(it) }
        numericOrdering?.let { collation = collation.numericOrdering(it) }
        alternate?.let { collation = collation.alternate(it) }
        maxVariable?.let { collation = collation.maxVariable(it) }
        backwards?.let { collation = collation.backwards(it) }
        normalization?.let { collation = collation.normalization(it) }

        return collation
    }
}