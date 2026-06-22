package de.lise.fluxflow.mongo.flowquery.expression.compilation.mapping

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.PropertyExpression
import de.fluxflow.flowquery.mapper.expression.ExpressionNode
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.ExpressionWalker.Companion.hasAnyDirectChildThat
import de.lise.fluxflow.mongo.query.getMongoFieldName

class ObjectIdReplacer : ExpressionReplacer {
    private val valueReplacer = ObjectIdValueReplacer()
        .recursive()
    
    override fun replace(node: ExpressionNode): Expression<*, *>? {
        val hasIdProperty = node.expression.hasAnyDirectChildThat {
            it is PropertyExpression<*, *, *> && toFieldName(it) == "_id" 
        }
        return when(hasIdProperty) {
            false -> null
            true -> valueReplacer.replace(node)
        }
    }

    private fun toFieldName(expression: Expression<*, *>): String? {
        return when (expression) {
            is PropertyExpression<*, *, *> -> expression.property.getMongoFieldName()
            else -> null
        }
    }
}