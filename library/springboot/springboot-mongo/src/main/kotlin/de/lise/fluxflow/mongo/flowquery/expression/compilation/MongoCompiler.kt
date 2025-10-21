package de.lise.fluxflow.mongo.flowquery.expression.compilation

import de.fluxflow.flowquery.expression.*
import de.fluxflow.flowquery.expression.compilation.CompilationException
import de.fluxflow.flowquery.expression.compilation.CompilationResult
import de.fluxflow.flowquery.expression.compilation.ExpressionCompiler
import de.lise.fluxflow.mongo.flowquery.expression.compilation.token.*
import org.bson.Document
import java.util.regex.Pattern
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.typeOf

internal typealias MongoCompilerResult = MongoToken

internal class MongoCompiler(
    private val subclassProvider: SubclassProvider
) : ExpressionCompiler<MongoCompilerResult> {
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
            is CastExpression<TRoot, *, *> -> doCompile(root, current.instance)
            is IsTypeExpression<TRoot, *, *> -> {
                val allKnownTypes = subclassProvider.findSubclasses(current.requiredType)
                StatementOperationToken(
                    ConvertingStatementToken(
                        doCompile(
                            root,
                            current.instance
                        ).toType<StatementToken>(
                            root,
                            current.instance
                        )
                    ) {
                        "${it}._class"
                    },
                    "in",
                    ConstantToken(
                        allKnownTypes.map { it.name }
                    )
                )
            }

            is BinaryOperationExpression<TRoot, *, *, *> -> when (current.operation) {
                else -> MatchToken(
                    StatementOperationToken(
                        doCompile(
                            root,
                            current.leftOperand
                        ).toType<StatementToken>(
                            root,
                            current.leftOperand
                        ),
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
                        doCompile(
                            root,
                            current.rightOperand
                        ).toType<ValueToken>(
                            root,
                            current.rightOperand
                        )
                    )
                )
            }

            is StartsWithExpression<TRoot> -> RegexToken(
                value = doCompile(
                    root,
                    current.value
                ).toType<StatementToken>(
                    root,
                    current.value
                ),
                ignoreCasing = current.ignoreCasing,
                pattern = ConvertingStatementToken(
                    doCompile(
                        root,
                        current.prefix
                    ).toType<StatementToken>(
                        root,
                        current.prefix
                    )
                ) {
                    "^${Pattern.quote(it)}.*"
                }
            )

            is EndsWithExpression<TRoot> -> RegexToken(
                value = doCompile(
                    root,
                    current.value
                ).toType<StatementToken>(
                    root,
                    current.value
                ),
                ignoreCasing = current.ignoreCasing,
                pattern = ConvertingStatementToken(
                    doCompile(
                        root,
                        current.suffix
                    ).toType<StatementToken>(
                        root,
                        current.suffix
                    )
                ) {
                    ".*${Pattern.quote(it)}$"
                }
            )

            is ContainsExpression<TRoot> -> RegexToken(
                value = doCompile(
                    root,
                    current.value
                ).toType<StatementToken>(
                    root,
                    current.value
                ),
                ignoreCasing = current.ignoreCasing,
                pattern = ConvertingStatementToken(
                    doCompile(
                        root,
                        current.substring
                    ).toType<StatementToken>(
                        root,
                        current.substring
                    )
                ) {
                    ".*${Pattern.quote(it)}.*"
                }
            )

            is ContainsElementThatExpression<TRoot, *, *> -> ExpressionTokenImpl(
                Document(
                    doCompile(
                        root,
                        current.collection
                    ).toType<StatementToken>(
                        root,
                        current.collection
                    ).toStatement(),
                    Document(
                        $$"$elemMatch",
                        doCompile(
                            Expression.root(),
                            current.elementPredicate
                        ).toType<ExpressionToken>(
                            root,
                            current.elementPredicate
                        ).toExpression()
                    )
                )
            )

            is ContainsElementExpression<TRoot, *, *> -> ExpressionTokenImpl(
                Document(
                    doCompile(
                        root,
                        current.collection
                    ).toType<StatementToken>(
                        root,
                        current.collection
                    ).toStatement(),
                    Document(
                        $$"$in",
                        listOf(
                            doCompile(
                                Expression.root(),
                                current.element
                            ).toType<ValueToken>(
                                root,
                                current.element
                            )
                                .toValue()
                        )
                    )
                )
            )

            is PropertyExpression<TRoot, *, *> -> PropertyToken(
                doCompile(
                    root,
                    current.instance
                ).toType<StatementToken>(
                    root,
                    current.instance
                ),
                current.property
            )

            is MapAccessExpression<TRoot, *, *, *> -> AnonymousPropertyToken(
                doCompile(
                    root,
                    current.instance
                ).toType<StatementToken>(
                    root,
                    current.instance
                ),
                doCompile(
                    root,
                    current.key
                ).toType<ValueToken>(
                    root,
                    current.key
                )
            )

            is RootExpression<*>, is ConjunctionExpression<TRoot, *> -> RootToken()
            is AndExpression<TRoot> -> AndToken(
                current.predicates.map {
                    doCompile(
                        root,
                        it
                    ).toType<ExpressionToken>(
                        root,
                        it
                    )
                }
            )

            is OrExpression<TRoot> -> OrToken(
                current.predicates.map {
                    doCompile(
                        root,
                        it
                    ).toType<ExpressionToken>(
                        root,
                        it
                    )
                }
            )

            is NotExpression<TRoot> -> NotToken(
                doCompile(
                    root,
                    current.expression
                ).toType<ExpressionToken>(
                    root,
                    current.expression
                )
            )

            is IsAnyOfOperator<TRoot, *> -> AnyOfToken(
                doCompile(
                    root,
                    current.valueToTest
                ).toType<StatementToken>(
                    root,
                    current.valueToTest
                ),
                current.anyOf.map { anyOfElement ->
                    doCompile(
                        root,
                        anyOfElement as Expression<TRoot, Any?>
                    ).toType<ValueToken>(
                        root,
                        anyOfElement
                    )
                }
            )

            is ConstantExpression -> ConstantToken(current.value)
        }
    }

    companion object {
        inline fun <reified T : MongoToken> MongoToken.toType(
            root: Expression<*, *>,
            current: Expression<*, *>,
            message: String? = null
        ): T {
            val directResult = this as? T
            if (directResult != null) {
                return directResult
            }

            if (this is ValueToken && StatementToken::class.isSubclassOf(T::class)) {
                val actualValue = this.toValue()
                if (actualValue is String) {
                    return SimpleStatementTokenImpl(actualValue) as T
                }
            }
            if (T::class.isSubclassOf(ExpressionToken::class) && this is MatchToken) {
                return this.expression as T
            }

            if (
                ExpressionToken::class.isSubclassOf(T::class)
                && this is PropertyToken
                && property.returnType in listOf(typeOf<Boolean>(), typeOf<Boolean?>())
            ) {
                return StatementOperationToken(
                    this,
                    "eq",
                    ConstantToken(true)
                ) as T
            }

            throw CompilationException(
                root,
                current,
                message ?: "Expected a ${T::class.simpleName}, but got ${this::class.simpleName}."
            )
        }
    }
}