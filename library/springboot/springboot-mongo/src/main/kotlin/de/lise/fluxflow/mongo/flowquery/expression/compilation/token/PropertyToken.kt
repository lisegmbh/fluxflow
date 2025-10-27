package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

import org.springframework.data.mapping.toDotPath
import kotlin.reflect.KProperty

internal class PropertyToken(
    val instance: StatementToken,
    val property: KProperty<*>
): StatementToken {
    override fun toStatement(): String {
        return when(instance) {
            is RootToken -> property.toDotPath()
            else -> "${instance.toStatement()}.${property.toDotPath()}"
        }
    }
}