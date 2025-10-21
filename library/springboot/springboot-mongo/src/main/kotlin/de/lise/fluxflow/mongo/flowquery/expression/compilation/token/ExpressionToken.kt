package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

import org.bson.Document

internal interface ExpressionToken : MongoToken {
    fun toExpression(): Document
}

