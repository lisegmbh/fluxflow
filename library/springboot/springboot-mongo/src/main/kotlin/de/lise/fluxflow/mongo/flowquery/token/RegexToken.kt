package de.lise.fluxflow.mongo.flowquery.token

import org.bson.Document

internal class RegexToken(
    private val value: StatementToken,
    private val pattern: StatementToken,
    private val ignoreCasing: Boolean
): ExpressionToken {
    override fun toExpression(): Document {
        return Document(
            value.toStatement(),
            Document(
                mapOf(
                    $$"$regex" to pattern.toStatement(),
                    $$"$options" to when (ignoreCasing) {
                        true -> "i"
                        else -> ""
                    }
                )
            )
        )
    }
}