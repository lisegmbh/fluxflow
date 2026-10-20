package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

import org.bson.Document

internal class StatementOperationToken(
    private val element: StatementToken,
    private val operation: String,
    private val valueToken: ValueToken
): ExpressionToken {
    override fun toExpression(): Document {
        return Document(
            element.toStatement(),
            Document(
                "$$operation",
                valueToken.toValue()
            )
        )
    }
}