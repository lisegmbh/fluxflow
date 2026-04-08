package de.lise.fluxflow.mongo.flowquery.expression.compilation

import de.fluxflow.flowquery.expression.*
import de.fluxflow.flowquery.expression.compilation.CompilationException
import de.fluxflow.flowquery.expression.compilation.CompilationResult
import de.fluxflow.flowquery.expression.compilation.ExpressionCompiler
import de.lise.fluxflow.mongo.flowquery.expression.compilation.mapping.MongoCompilerMapper
import de.lise.fluxflow.mongo.flowquery.expression.compilation.mapping.MongoCompilerMapperImpl
import de.lise.fluxflow.mongo.flowquery.expression.compilation.token.*
import org.bson.Document
import org.slf4j.LoggerFactory

internal class MongoCompiler(
    private val subclassProvider: SubclassProvider,
    private val expressionMapper: MongoCompilerMapper = MongoCompilerMapperImpl(),
    private val config: MongoCompilerConfig = MongoCompilerConfig()
) : ExpressionCompiler<MongoToken> {

    
    private val logger = LoggerFactory.getLogger(MongoCompiler::class.java)

    override fun <TRoot, TResult> compile(
        expression: Expression<TRoot, TResult>
    ): CompilationResult<MongoToken> {
        val mappedExpression = expressionMapper.map(expression)
        return try {
            val result = doCompile(mappedExpression, mappedExpression)
            if (logger.isTraceEnabled) {
                logger.trace("Successfully compiled expression {} to MongoDB token: {}", mappedExpression, result)
            }
            CompilationResult(result)
        } catch (e: Exception) {
            logger.error("Failed to compile expression: {}", mappedExpression, e)
            throw CompilationException(mappedExpression, mappedExpression, "Compilation failed: ${e.message}")
        }
    }

    private fun <TRoot, TCurrent> doCompile(
        root: Expression<TRoot, *>,
        current: Expression<TRoot, TCurrent>
    ): MongoToken {
        if (logger.isTraceEnabled) {
            logger.trace("Compiling expression: {} (type: {})", current, current::class.simpleName)
        }

        return when (current) {
            is CastExpression<TRoot, *, *> -> doCompile(root, current.instance)
            is IsTypeExpression<TRoot, *, *> -> handleIsTypeExpression(root, current)
            is BinaryOperationExpression<TRoot, *, *> -> handleBinaryOperation(root, current)
            is StartsWithExpression<TRoot> -> handleStartsWithExpression(root, current)
            is EndsWithExpression<TRoot> -> handleEndsWithExpression(root, current)
            is ContainsExpression<TRoot> -> handleContainsExpression(root, current)
            is ContainsElementThatExpression<TRoot, *, *> -> handleContainsElementThatExpression(root, current)
            is ContainsElementExpression<TRoot, *, *> -> handleContainsElementExpression(root, current)
            is PropertyExpression<TRoot, *, *> -> handlePropertyExpression(root, current)
            is MapAccessExpression<TRoot, *, *, *> -> handleMapAccessExpression(root, current)
            is HasKeyExpression<TRoot, *> -> handleHasKeyExpression(root, current)
            is RootExpression<*>, is ConjunctionExpression<TRoot, *> -> RootToken()
            is AndExpression<TRoot> -> handleAndExpression(root, current)
            is OrExpression<TRoot> -> handleOrExpression(root, current)
            is NotExpression<TRoot> -> handleNotExpression(root, current)
            is IsAnyOfOperator<TRoot, *> -> handleIsAnyOfOperator(root, current)
            is ConstantExpression -> ConstantToken(current.value)
        }
    }

    private fun <TRoot> handleIsTypeExpression(
        root: Expression<TRoot, *>,
        current: IsTypeExpression<TRoot, *, *>
    ): MongoToken {
        val allKnownTypes = subclassProvider.findSubclasses(current.requiredType)
        val instanceToken = doCompile(root, current.instance).asStatementToken(root, current.instance)

        return StatementOperationToken(
            ConvertingStatementToken(instanceToken) { "${it}.${config.typeFieldName}" },
            "in",
            ConstantToken(allKnownTypes.map { it.name })
        )
    }

    private fun <TRoot> handleBinaryOperation(
        root: Expression<TRoot, *>,
        current: BinaryOperationExpression<TRoot, *, *>
    ): MongoToken {
        val leftToken = doCompile(root, current.leftOperand).asStatementToken(root, current.leftOperand)
        val rightToken = doCompile(root, current.rightOperand).asValueToken(root, current.rightOperand)

        val operator = when (current.operation) {
            BinaryOperation.Equal -> "eq"
            BinaryOperation.NotEqual -> "ne"
            BinaryOperation.LessThan -> "lt"
            BinaryOperation.LessThanOrEqual -> "lte"
            BinaryOperation.GreaterThan -> "gt"
            BinaryOperation.GreaterThanOrEqual -> "gte"
        }

        return MatchToken(StatementOperationToken(leftToken, operator, rightToken))
    }

    private fun <TRoot> handleStartsWithExpression(
        root: Expression<TRoot, *>,
        current: StartsWithExpression<TRoot>
    ): MongoToken {
        val valueToken = doCompile(root, current.value).asStatementToken(root, current.value)
        val prefixToken = doCompile(root, current.prefix).asStatementToken(root, current.prefix)

        return RegexToken.startsWith(valueToken, prefixToken, current.ignoreCasing)
    }

    private fun <TRoot> handleEndsWithExpression(
        root: Expression<TRoot, *>,
        current: EndsWithExpression<TRoot>
    ): MongoToken {
        val valueToken = doCompile(root, current.value).asStatementToken(root, current.value)
        val suffixToken = doCompile(root, current.suffix).asStatementToken(root, current.suffix)

        return RegexToken.endsWith(valueToken, suffixToken, current.ignoreCasing)
    }

    private fun <TRoot> handleContainsExpression(
        root: Expression<TRoot, *>,
        current: ContainsExpression<TRoot>
    ): MongoToken {
        val valueToken = doCompile(root, current.value).asStatementToken(root, current.value)
        val substringToken = doCompile(root, current.substring).asStatementToken(root, current.substring)

        return RegexToken.contains(valueToken, substringToken, current.ignoreCasing)
    }

    private fun <TRoot> handleContainsElementThatExpression(
        root: Expression<TRoot, *>,
        current: ContainsElementThatExpression<TRoot, *, *>
    ): MongoToken {
        val collectionToken = doCompile(root, current.collection).asStatementToken(root, current.collection)
        val predicateToken = doCompile(Expression.root(), current.elementPredicate)
            .asExpressionToken(root, current.elementPredicate)

        return ExpressionTokenImpl(
            Document(
                collectionToken.toStatement(),
                Document("\$elemMatch", predicateToken.toExpression())
            )
        )
    }

    private fun <TRoot> handleContainsElementExpression(
        root: Expression<TRoot, *>,
        current: ContainsElementExpression<TRoot, *, *>
    ): MongoToken {
        val collectionToken = doCompile(root, current.collection).asStatementToken(root, current.collection)
        val elementToken = doCompile(Expression.root(), current.element)
            .asValueToken(root, current.element)

        return ExpressionTokenImpl(
            Document(
                collectionToken.toStatement(),
                Document("\$in", listOf(elementToken.toValue()))
            )
        )
    }

    private fun <TRoot> handlePropertyExpression(
        root: Expression<TRoot, *>,
        current: PropertyExpression<TRoot, *, *>
    ): MongoToken {
        val instanceToken = doCompile(root, current.instance).asStatementToken(root, current.instance)
        return PropertyToken(instanceToken, current.property)
    }

    private fun <TRoot> handleMapAccessExpression(
        root: Expression<TRoot, *>,
        current: MapAccessExpression<TRoot, *, *, *>
    ): MongoToken {
        val instanceToken = doCompile(root, current.instance).asStatementToken(root, current.instance)
        val keyToken = doCompile(root, current.key).asValueToken(root, current.key)
        return AnonymousPropertyToken(instanceToken, keyToken)
    }

    private fun <TRoot> handleHasKeyExpression(
        root: Expression<TRoot, *>,
        current: HasKeyExpression<TRoot, *>
    ): MongoToken {
        val instanceToken = doCompile(root, current.instance).asStatementToken(root, current.instance)
        val keyToken = doCompile(root, current.key).asValueToken(root, current.key)
        
        return MatchToken(
            StatementOperationToken(
                AnonymousPropertyToken(
                    instanceToken,
                    keyToken
                ),
                "exists",
                ConstantToken(true)
            )
        )
    }
    
    private fun <TRoot> handleAndExpression(
        root: Expression<TRoot, *>,
        current: AndExpression<TRoot>
    ): MongoToken {
        val predicateTokens = current.predicates.map { predicate ->
            doCompile(root, predicate).asExpressionToken(root, predicate)
        }
        return AndToken(predicateTokens)
    }

    private fun <TRoot> handleOrExpression(
        root: Expression<TRoot, *>,
        current: OrExpression<TRoot>
    ): MongoToken {
        val predicateTokens = current.predicates.map { predicate ->
            doCompile(root, predicate).asExpressionToken(root, predicate)
        }
        return OrToken(predicateTokens)
    }

    private fun <TRoot> handleNotExpression(
        root: Expression<TRoot, *>,
        current: NotExpression<TRoot>
    ): MongoToken {
        val expressionToken = doCompile(root, current.expression).asExpressionToken(root, current.expression)
        return NotToken(expressionToken)
    }

    private fun <TRoot> handleIsAnyOfOperator(
        root: Expression<TRoot, *>,
        current: IsAnyOfOperator<TRoot, *>
    ): MongoToken {
        val valueToken = doCompile(root, current.valueToTest).asStatementToken(root, current.valueToTest)
        val valueTokens = current.anyOf.map { anyOfElement ->
            @Suppress("UNCHECKED_CAST")
            doCompile(root, anyOfElement as Expression<TRoot, Any?>).asValueToken(root, anyOfElement)
        }
        return AnyOfToken(valueToken, valueTokens)
    }
}
