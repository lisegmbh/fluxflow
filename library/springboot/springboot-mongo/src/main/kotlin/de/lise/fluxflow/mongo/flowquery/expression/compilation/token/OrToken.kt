package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

import org.bson.Document

internal class OrToken(
    private val expressionTokens: List<ExpressionToken>
): ExpressionToken {
    override fun toExpression(): Document {
        return Document(
            $$"$or",
            expressionTokens.map { it.toExpression() }
        )
    }
}