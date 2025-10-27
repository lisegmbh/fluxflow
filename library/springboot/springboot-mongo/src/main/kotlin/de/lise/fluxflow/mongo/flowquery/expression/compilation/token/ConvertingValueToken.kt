package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

internal data class ConvertingValueToken(
    val source: ValueToken,
    val mapper: (originalValue: Any?) -> Any?
): ValueToken {
    override fun toValue(): Any? {
        return mapper(
            source.toValue()
        )
    }
}