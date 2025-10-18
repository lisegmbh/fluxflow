package de.lise.fluxflow.mongo.flowquery.token

import org.bson.Document

internal class IsEqualToken(
    val left: StatementToken,
    val right: ValueToken
) : ExpressionToken {
    override fun toExpression(): Document {
        return Document(
            left.toStatement(),
            right.toValue()
        )
    }
}