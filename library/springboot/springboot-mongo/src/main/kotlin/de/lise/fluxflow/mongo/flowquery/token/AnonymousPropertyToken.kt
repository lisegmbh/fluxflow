package de.lise.fluxflow.mongo.flowquery.token

internal class AnonymousPropertyToken(
    val instance: StatementToken,
    val property: ValueToken
): StatementToken {
    override fun toStatement(): String {
        val propertyName = property.toValue().toString()
        return when(instance) {
            is RootToken -> propertyName
            else -> "${instance.toStatement()}.$propertyName"
        }
    }
}