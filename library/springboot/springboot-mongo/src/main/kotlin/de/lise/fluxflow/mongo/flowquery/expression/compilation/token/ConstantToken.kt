package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

internal class ConstantToken(
    val value: Any?
) : ValueToken {
    override fun toValue(): Any? {
        return value
    }
}

