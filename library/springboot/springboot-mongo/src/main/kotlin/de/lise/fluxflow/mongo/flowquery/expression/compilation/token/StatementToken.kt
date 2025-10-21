package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

internal interface StatementToken : MongoToken, ValueToken {
    fun toStatement(): String

    override fun toValue(): Any {
        return toStatement()
    }
}