package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

class SimpleStatementTokenImpl(
    val value: String
): StatementToken {
    override fun toStatement(): String {
        return value
    }
}