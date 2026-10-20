package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.compilation.CompilationException
import kotlin.reflect.typeOf

/**
 * Base interface for all MongoDB compilation tokens.
 * Provides a unified type hierarchy with safe conversion methods.
 */
internal sealed interface MongoToken {

    /**
     * Safely converts this token to a StatementToken
     */
    fun asStatementToken(root: Expression<*, *>, current: Expression<*, *>): StatementToken {
        return when (this) {
            is StatementToken -> this
            is ValueToken -> {
                val value = this.toValue()
                if (value is String) {
                    SimpleStatementTokenImpl(value)
                } else {
                    throw CompilationException(
                        root,
                        current,
                        "Cannot convert ValueToken with non-string value '$value' to StatementToken"
                    )
                }
            }
            else -> throw CompilationException(
                root, current,
                "Cannot convert ${this::class.simpleName} to StatementToken"
            )
        }
    }

    /**
     * Safely converts this token to a ValueToken
     */
    fun asValueToken(root: Expression<*, *>, current: Expression<*, *>): ValueToken {
        return when (this) {
            is ValueToken -> this
            else -> throw CompilationException(
                root, current,
                "Cannot convert ${this::class.simpleName} to ValueToken"
            )
        }
    }

    /**
     * Safely converts this token to an ExpressionToken
     */
    fun asExpressionToken(root: Expression<*, *>, current: Expression<*, *>): ExpressionToken {
        return when (this) {
            is ExpressionToken -> this
            is MatchToken -> this.expression
            is PropertyToken -> {
                if (
                    property.returnType == typeOf<Boolean>() ||
                    property.returnType == typeOf<Boolean?>()
                ) {
                    StatementOperationToken(this, "eq", ConstantToken(true))
                } else {
                    throw CompilationException(
                        root, current,
                        "Cannot convert non-boolean PropertyToken to ExpressionToken"
                    )
                }
            }
            else -> throw CompilationException(
                root, current,
                "Cannot convert ${this::class.simpleName} to ExpressionToken"
            )
        }
    }
}

