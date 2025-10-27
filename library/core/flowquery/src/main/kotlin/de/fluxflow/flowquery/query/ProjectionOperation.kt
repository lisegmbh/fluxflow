package de.fluxflow.flowquery.query

import de.fluxflow.flowquery.expression.Expression

data class ProjectionOperation(
    val projection: Expression<*, *>
): QueryOperation {
    override fun toText(): String {
        return "SELECT ${projection.toText()}"
    }

    override fun toString(): String {
        return toText()
    }
}