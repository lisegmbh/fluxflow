package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

internal interface ValueToken : MongoToken {
    fun toValue(): Any?
}

