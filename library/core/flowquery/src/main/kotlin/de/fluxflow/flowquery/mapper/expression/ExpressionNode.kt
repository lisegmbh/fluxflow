package de.fluxflow.flowquery.mapper.expression

import de.fluxflow.flowquery.expression.Expression
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A single unit of work for expression tree traversal and transformation.
 *
 * An [ExpressionNode] pairs an [Expression] with the [expectedType] that its surrounding
 * context requires the expression to resolve to. The expected type gives replacers and
 * mappers additional information to choose the correct rewrite — for example, a filter
 * predicate is wrapped in a node whose expected type is [Boolean].
 *
 * Nodes are passed to [ExpressionReplacer.replace] and [ExpressionMapper.map] and are
 * carried through tree traversal by [ExpressionWalker] (via [WalkingContext]).
 *
 * @param expression the expression represented by this node
 * @param expectedType the type the surrounding context expects this expression to produce,
 * or `null` when no particular type is expected
 */
data class ExpressionNode(
    val expression: Expression<*, *>,
    val expectedType: KType? = null,
) {

    /**
     * Returns a copy of this node whose [expectedType] is set to the reified type [T].
     *
     * @param T the type the context expects this expression to produce
     * @return a copy of this node with [expectedType] set to the [KType] of [T]
     */
    inline fun <reified T> withExpectedType(): ExpressionNode {
        return copy(
             expectedType = typeOf<T>()
        )
    }

    companion object {
        /**
         * Creates a root node for the given [expression] without an expected type.
         *
         * This is the usual entry point when starting traversal or transformation of an
         * expression (sub)tree.
         *
         * @param expression the expression to wrap
         * @return a new [ExpressionNode] with no [expectedType]
         */
        fun root(
            expression: Expression<*, *>
        ): ExpressionNode {
            return ExpressionNode(
                expression,
                null,
            )
        }
    }
}