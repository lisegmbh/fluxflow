package de.lise.fluxflow.mongo.flowquery.token

internal interface ValueToken : MongoToken {
    fun toValue(): Any?
}

