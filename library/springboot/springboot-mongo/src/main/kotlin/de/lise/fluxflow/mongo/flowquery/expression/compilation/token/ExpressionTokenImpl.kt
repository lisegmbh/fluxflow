package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

import org.bson.Document

internal data class ExpressionTokenImpl(
    private val expression: Document
) : ExpressionToken {
    override fun toExpression(): Document {
        return expression
    }
}