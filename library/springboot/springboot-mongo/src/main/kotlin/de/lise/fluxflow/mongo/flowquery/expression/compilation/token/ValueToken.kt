package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

/**
 * Represents a token that can be converted to a MongoDB value
 */
internal interface ValueToken : MongoToken {
    fun toValue(): Any?
}