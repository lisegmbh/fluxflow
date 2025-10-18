package de.lise.fluxflow.mongo.flowquery.token

import org.bson.Document

internal class NotToken(
    private val notToken: ExpressionToken
): ExpressionToken {
    override fun toExpression(): Document {
        return Document(
            $$"$nor",
            listOf(notToken.toExpression())
        )
    }
}