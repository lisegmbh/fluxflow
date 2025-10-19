package de.lise.fluxflow.mongo.flowquery.token

class SimpleStatementTokenImpl(
    val value: String
): StatementToken {
    override fun toStatement(): String {
        return value
    }
}