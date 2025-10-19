package de.lise.fluxflow.mongo.flowquery.token

internal data class ConvertingStatementToken(
    val source: StatementToken,
    val mapper: (originalValue: String) -> String
): StatementToken {
    override fun toStatement(): String {
        return mapper(source.toStatement())
    }
}