package de.lise.fluxflow.springboot.rest.filter

import de.fluxflow.flowquery.expression.*
import de.fluxflow.flowquery.expression.ExpressionExtensions.Logical.not
import de.lise.fluxflow.springboot.odata.filter.grammar.ODataFilterParser
import org.antlr.v4.runtime.tree.TerminalNode
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

@Suppress("UNCHECKED_CAST")
class ODataTreeBuilder<TElement : Any>(
    private val elementKind: KClass<TElement>
) {
    fun build(
        lexerTree: ODataFilterParser.FilterContext
    ): Expression<TElement, *> {
        val result = doBuild(
            lexerTree.expr()
        )

        return result
    }

    private fun doBuild(expr: ODataFilterParser.ExprContext): Expression<TElement, *> {
        return doBuildOr(expr.orExpr())
    }

    private fun doBuildOr(orExpr: ODataFilterParser.OrExprContext): PredicateExpression<TElement> {
        val orExpressions = orExpr.andExpr().map {
            doBuildAnd(it)
        }
        if (orExpressions.size == 1) {
            return orExpressions.single()
        }
        return OrExpression(
            orExpressions
        )
    }

    private fun doBuildAnd(andExpr: ODataFilterParser.AndExprContext): PredicateExpression<TElement> {
        val andExpressions = andExpr.notExpr().map {
            doBuildNot(it)
        }

        if (andExpressions.size == 1) {
            return andExpressions.single()
        }

        return AndExpression(
            andExpressions
        )
    }

    private fun doBuildNot(context: ODataFilterParser.NotExprContext): PredicateExpression<TElement> {
        val notExpression = context.notExpr()
        return if (notExpression != null) {
            doBuildNot(notExpression).not()
        } else if (context.comparisonExpr() != null) {
            doBuildComparison(context.comparisonExpr())
        } else {
            doBuildInExpression(context.inExpr())
        }
    }

    private fun doBuildInExpression(
        inExpr: ODataFilterParser.InExprContext
    ): PredicateExpression<TElement> {
        val leftHand: Expression<TElement, *> = doBuildAdditiveExpr(
            inExpr.additiveExpr()
        )
        val valueList = doBuildValueList(
            inExpr.argumentList()
        )

        return IsAnyOfOperator(
            leftHand as Expression<TElement, Any?>,
            valueList
        )
    }

    private fun doBuildValueList(
        argumentList: ODataFilterParser.ArgumentListContext
    ): Set<Expression<TElement, Any?>> {
        return argumentList.expr().map { argument ->
            doBuild(argument) as Expression<TElement, Any?>
        }.toSet()
    }

    private fun doBuildComparison(comparisonExpr: ODataFilterParser.ComparisonExprContext): PredicateExpression<TElement> {
        val a1: ODataFilterParser.AdditiveExprContext = comparisonExpr.additiveExpr(0)
        val leftHand = doBuildAdditiveExpr(a1)

        if (comparisonExpr.compOp() == null) {
            return leftHand as PredicateExpression<TElement>
        }

        val comparisonOp = comparisonExpr.compOp()
        val a2 = comparisonExpr.additiveExpr(1)
        val rightHand = doBuildAdditiveExpr(a2)

        return if (comparisonOp.EQ() != null) {
            leftHand.isEqual(
                rightHand
            )
        } else if (comparisonOp.NE() != null) {
            leftHand.isNotEqual(
                rightHand
            )
        } else if (comparisonOp.GT() != null) {
            (leftHand as Expression<TElement, Comparable<Any>>).isGreaterThan(
                rightHand as Expression<TElement, Comparable<Any>>
            )

        } else if (comparisonOp.LT() != null) {
            (leftHand as Expression<TElement, Comparable<Any>>).isLessThan(
                rightHand as Expression<TElement, Comparable<Any>>
            )
        } else if (comparisonOp.GE() != null) {
            (leftHand as Expression<TElement, Comparable<Any>>).isGreaterThanOrEqual(
                rightHand as Expression<TElement, Comparable<Any>>
            )

        } else if (comparisonOp.LE() != null) {
            (leftHand as Expression<TElement, Comparable<Any>>).isLessThanOrEqual(
                rightHand as Expression<TElement, Comparable<Any>>
            )
        } else {
            throw ExpressionParsingException(
                comparisonOp,
                "Unknown or unsupported comparison operation '${comparisonOp.text}'."
            )
        }
    }

    private fun doBuildAdditiveExpr(a1: ODataFilterParser.AdditiveExprContext): Expression<TElement, *> {
        return doBuildPrimaryExpr(a1.primaryExpr())
    }

    private fun doBuildPrimaryExpr(
        primaryExpr: ODataFilterParser.PrimaryExprContext
    ): Expression<TElement, *> {
        if (primaryExpr.literal() != null) {
            return doBuildLiteral(primaryExpr.literal())
        } else if (primaryExpr.member() != null) {
            return doBuildMember(
                primaryExpr.member(),
                Expression.Companion.root()
            )
        } else if (primaryExpr.expr() != null) {
            return doBuild(
                primaryExpr.expr()
            )
        } else if (primaryExpr.functionCall() != null) {
            return doBuildFunction(
                primaryExpr.functionCall()
            )
        }

        throw ExpressionParsingException(
            primaryExpr,
            "Failed parsing unknown primary expression: ${primaryExpr.text}"
        )
    }

    private fun doBuildFunction(
        functionCall: ODataFilterParser.FunctionCallContext
    ): Expression<TElement, *> {
        val arguments = functionCall.argumentList()?.expr()?.map { arg ->
            doBuild(arg)
        } ?: emptyList()

        val functionName = functionCall.FUN().text

        return when (functionName) {
            "contains" -> {
                val main = arguments.getOrNull(0)!!
                val sub = arguments.getOrNull(1)!!

                ContainsExpression(
                    main as Expression<TElement, String>,
                    sub as Expression<TElement, String>,
                    false
                )
            }

            else -> throw ExpressionParsingException(
                functionCall.FUN().symbol,
                "Function '${functionCall.text}' is not supported."
            )
        }
    }

    private fun doBuildMember(
        member: ODataFilterParser.MemberContext,
        expression: Expression<TElement, TElement>
    ): Expression<TElement, *> {
        val path = member.IDENTIFIER() as List<TerminalNode>

        return doBuildPath(
            expression,
            elementKind,
            LinkedList(path)
        )
    }

    private fun <TCurrent : Any> doBuildPath(
        expression: Expression<TElement, TCurrent>,
        currentType: KClass<TCurrent>,
        remaining: Queue<TerminalNode>
    ): Expression<TElement, *> {
        if (remaining.isEmpty()) {
            return expression
        }
        val currentPath = remaining.poll()
        val foundProperty = currentType.memberProperties.firstOrNull {
            it.name == currentPath.text
        }

        if (foundProperty != null) {
            val nextExpression: PropertyExpression<TElement, TCurrent, Any> = expression.get(foundProperty)
            val nextType: KClass<Any> = foundProperty.returnType.classifier as KClass<Any>
            return doBuildPath(
                nextExpression,
                nextType,
                remaining
            )
        }

        throw ExpressionParsingException(
            currentPath.symbol,
            "Property '${currentPath.text}' could not be found on ${currentType.simpleName}."
        )
    }

    private fun doBuildLiteral(
        literal: ODataFilterParser.LiteralContext
    ): Expression<TElement, *> {
        return if (literal.NULL() != null) {
            Expression.const(null)
        } else if (literal.STRING() != null) {
            val rawValue = literal.STRING().text
            val unescapedValue = rawValue.substring(1, rawValue.length - 1).replace("''", "'")
            Expression.Companion.const(unescapedValue)
        } else if (literal.NUMBER() != null) {
            val rawValue = literal.NUMBER().text.trim()
            if (rawValue.contains('.')) {
                Expression.const(
                    rawValue.toDouble()
                )
            } else {
                Expression.const(
                    rawValue.toInt()
                )
            }
        } else if (literal.BOOLEAN() != null) {
            val rawValue = literal.BOOLEAN().text.trim()
            Expression.const(
                rawValue.toBoolean()
            )
        } else {
            throw ExpressionParsingException(
                literal,
                "Could not convert '${literal.text}' to a literal."
            )
        }
    }
}