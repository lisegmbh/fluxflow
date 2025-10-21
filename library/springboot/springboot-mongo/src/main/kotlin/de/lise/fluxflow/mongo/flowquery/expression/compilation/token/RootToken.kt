package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

internal class RootToken : StatementToken {
    override fun toStatement(): String {
        return ""
    }
}