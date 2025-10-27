package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

import org.bson.Document

/**
 * Represents a token that can be converted to a MongoDB query expression
 */
internal interface ExpressionToken : MongoToken {
    fun toExpression(): Document
}