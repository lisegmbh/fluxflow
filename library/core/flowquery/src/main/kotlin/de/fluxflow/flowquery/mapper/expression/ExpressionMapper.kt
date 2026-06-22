package de.fluxflow.flowquery.mapper.expression

import de.fluxflow.flowquery.expression.Expression

/**
 * Transforms an [ExpressionNode] into a (possibly rewritten) [Expression].
 *
 * In contrast to [ExpressionReplacer], a mapper is a *total* transformation: it always
 * returns an [Expression]. When no rewrite applies, implementations are expected to return
 * the node's original [ExpressionNode.expression] unchanged.
 *
 * A mapper is typically obtained from an [ExpressionReplacer] via [ExpressionReplacer.toMapper]
 * and is used to translate a complete query expression tree from one representation into another
 * (for example, from domain types to their persisted data representation).
 *
 * As a Kotlin `fun interface`, a mapper can be created directly from a lambda:
 * ```
 * val mapper = ExpressionMapper { node -> node.expression }
 * ```
 */
fun interface ExpressionMapper {
    /**
     * Maps the given [node] to a (possibly transformed) [Expression].
     *
     * @param node the expression node to transform
     * @return the resulting expression; the original [ExpressionNode.expression] if no
     * transformation applies
     */
    fun map(node: ExpressionNode): Expression<*,*>
}