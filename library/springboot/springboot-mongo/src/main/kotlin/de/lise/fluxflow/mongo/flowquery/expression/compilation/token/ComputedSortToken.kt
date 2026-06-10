package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

import org.bson.Document

/**
 * Sort by a computed aggregation expression, materialized via `$addFields` before `$sort`.
 */
internal data class ComputedSortToken(
    val fieldName: String,
    val expression: Document,
) : MongoSortToken