package de.lise.fluxflow.mongo.flowquery.expression.compilation.mapping

import de.fluxflow.flowquery.expression.ConstantExpression
import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.ExpressionNode
import org.bson.types.ObjectId

class ObjectIdValueReplacer : ExpressionReplacer {
    override fun replace(node: ExpressionNode): Expression<*, *>? {
        return when (val exp = node.expression) {
            is ConstantExpression<*, *> -> {
                when (val value = exp.value) {
                    is String -> when (ObjectId.isValid(value)) {
                        true -> ConstantExpression<Any, ObjectId>(
                            ObjectId(value)
                        )

                        false -> null
                    }

                    else -> null
                }
            }

            else -> null
        }
    }
}