package de.lise.fluxflow.mongo.flowquery

import de.fluxflow.flowquery.expression.*
import de.lise.fluxflow.mongo.flowquery.token.*
import java.util.regex.Pattern
import kotlin.reflect.full.isSubclassOf

internal typealias MongoCompilerResult = MongoToken

internal class MongoCompiler : ExpressionCompiler<MongoCompilerResult> {
    override fun <TRoot, TResult> compile(
        expression: Expression<TRoot, TResult>
    ): CompilationResult<MongoCompilerResult> {
        return CompilationResult(
            doCompile(
                expression,
                expression
            )
        )
    }


    private fun <TRoot, TCurrent> doCompile(
        root: Expression<TRoot, *>,
        current: Expression<TRoot, TCurrent>
    ): MongoToken {
        return when (current) {
            is BinaryOperationExpression<TRoot, *, *, *> -> when (current.operation) {
                else -> MatchToken(
                    StatementOperationToken(
                        doCompile(root, current.leftOperand).toType<StatementToken>(root, current.leftOperand),
                        when (current.operation) {
                            BinaryOperation.Equal -> "eq"
                            BinaryOperation.NotEqual -> "ne"
                            BinaryOperation.LessThan -> "lt"
                            BinaryOperation.LessThanOrEqual -> "lte"
                            BinaryOperation.GreaterThan -> "gt"
                            BinaryOperation.GreaterThanOrEqual -> "gte"
                            else -> throw CompilationException(
                                root,
                                current,
                                "Unsupported expression of type '${current::class.simpleName}'."
                            )
                        },
                        doCompile(root, current.rightOperand).toType<ValueToken>(root, current.rightOperand)
                    )
                )
            }

            is StartsWithExpression<TRoot> -> RegexToken(
                value = doCompile(root, current.value).toType<StatementToken>(root, current.value),
                ignoreCasing = current.ignoreCasing,
                pattern = ConvertingStatementToken(
                    doCompile(root, current.prefix).toType<StatementToken>(root, current.value)
                ) {
                   "^${Pattern.quote("$it")}.*"
                }
            )

            is EndsWithExpression<TRoot> -> RegexToken(
                value = doCompile(root, current.value).toType<StatementToken>(root, current.value),
                ignoreCasing = current.ignoreCasing,
                pattern = ConvertingStatementToken(
                    doCompile(root, current.suffix).toType<StatementToken>(root, current.value)
                ) {
                    ".*${Pattern.quote("$it")}$"
                }
            )

            is ContainsExpression<TRoot> -> RegexToken(
                value = doCompile(root, current.value).toType<StatementToken>(root, current.value),
                ignoreCasing = current.ignoreCasing,
                pattern = ConvertingStatementToken(
                    doCompile(root, current.substring).toType<StatementToken>(root, current.value)
                ) {
                    ".*${Pattern.quote("$it")}.*"
                }
            )

            is PropertyExpression<TRoot, *, *> -> PropertyToken(
                doCompile(root, current.instance).toType<StatementToken>(root, current.instance),
                current.property
            )

            is Root<*>, is ConjunctionExpression<TRoot, *> -> RootToken()
            is AndOperator<TRoot> -> AndToken(
                current.predicates.map {
                    doCompile(root, it).toType<ExpressionToken>(root, it)
                }
            )

            is OrOperator<TRoot> -> OrToken(
                current.predicates.map {
                    doCompile(root, it).toType<ExpressionToken>(root, it)
                }
            )

            is NotOperator<TRoot> -> NotToken(
                doCompile(root, current.expression).toType<ExpressionToken>(root, current.expression)
            )

            is IsAnyOfOperator<TRoot, *> -> AnyOfToken(
                doCompile(root, current.valueToTest).toType<StatementToken>(root, current.valueToTest),
                current.anyOf.map { anyOfElement ->
                    doCompile(
                        root,
                        anyOfElement as Expression<TRoot, Any?>
                    ).toType<ValueToken>(root, anyOfElement)
                }
            )

            is Constant -> ConstantToken(current.value)
        }
    }

    companion object {
        inline fun <reified T : MongoToken> MongoToken.toType(
            root: Expression<*, *>,
            current: Expression<*, *>,
            message: String? = null
        ): T {
            val directResult = this as? T
            if(directResult != null) {
                return directResult
            }

            if(this is ValueToken && StatementToken::class.isSubclassOf(T::class)) {
                val actualValue = this.toValue()
                if(actualValue is String) {
                    return SimpleStatementTokenImpl(actualValue) as T
                }
            }
            if (T::class.isSubclassOf(ExpressionToken::class) && this is MatchToken) {
                return this.expression as T
            }

            throw CompilationException(
                root,
                current,
                message ?: "Expected a ${T::class.simpleName}, but got ${this::class.simpleName}."
            )
        }
    }
}