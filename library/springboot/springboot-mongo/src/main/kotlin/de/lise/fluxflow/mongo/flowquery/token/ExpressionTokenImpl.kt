package de.lise.fluxflow.mongo.flowquery.token

import org.bson.Document

internal data class ExpressionTokenImpl(
    private val expression: Document
) : ExpressionToken {
    override fun toExpression(): Document {
        return expression
    }
}