package de.lise.fluxflow.mongo.flowquery.token

import org.bson.Document

internal class AnyOfToken(
    private val element: StatementToken,
    private val values: Collection<ValueToken>
): ExpressionToken {
    override fun toExpression(): Document {
        return Document(
            element.toStatement(),
            Document(
                $$"$in",
                values.map { it.toValue() }
            )
        )
    }
}