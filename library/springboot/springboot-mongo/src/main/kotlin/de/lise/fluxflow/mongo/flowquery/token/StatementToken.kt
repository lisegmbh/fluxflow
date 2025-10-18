package de.lise.fluxflow.mongo.flowquery.token

internal interface StatementToken : MongoToken, ValueToken {
    fun toStatement(): String

    override fun toValue(): Any {
        return toStatement()
    }
}