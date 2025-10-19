package de.lise.fluxflow.mongo.flowquery.token

internal class ConstantToken(
    val value: Any?
) : ValueToken {
    override fun toValue(): Any? {
        return value
    }
}

