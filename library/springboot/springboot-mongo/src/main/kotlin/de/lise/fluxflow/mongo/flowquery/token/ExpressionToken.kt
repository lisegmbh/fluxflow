package de.lise.fluxflow.mongo.flowquery.token

import org.bson.Document

internal interface ExpressionToken : MongoToken {
    fun toExpression(): Document
}