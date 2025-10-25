package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

/**
 * Represents a token that can be converted to a MongoDB field path or statement
 */
internal interface StatementToken : ValueToken {
    fun toStatement(): String

    override fun toValue(): Any {
        return toStatement()
    }
}