package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

/**
 * Sort by an existing document field path.
 */
internal data class PathSortToken(
    val path: StatementToken,
) : MongoSortToken