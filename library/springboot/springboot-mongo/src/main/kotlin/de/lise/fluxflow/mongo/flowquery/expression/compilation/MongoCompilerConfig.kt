package de.lise.fluxflow.mongo.flowquery.expression.compilation

/**
 * Configuration for MongoDB compilation behavior
 */
data class MongoCompilerConfig(
    val typeFieldName: String = "_class"
)