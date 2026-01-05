package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

import de.lise.fluxflow.mongo.query.getMongoFieldName
import kotlin.reflect.KProperty

internal class PropertyToken(
    val instance: StatementToken,
    val property: KProperty<*>
): StatementToken {
    override fun toStatement(): String {
        return when(instance) {
            is RootToken -> property.getMongoFieldName()
            else -> "${instance.toStatement()}.${property.getMongoFieldName()}"
        }
    }
}