package de.lise.fluxflow.mongo.flowquery.token

import org.bson.Document

internal class AndToken(
    private val expressionTokens: List<ExpressionToken>
) : ExpressionToken {
    override fun toExpression(): Document {
        return Document(
            $$"$and",
            expressionTokens.map { it.toExpression() }
        )
    }
}