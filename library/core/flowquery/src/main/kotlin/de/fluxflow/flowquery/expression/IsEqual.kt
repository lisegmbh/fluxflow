package de.fluxflow.flowquery.expression

class IsEqual<TRoot, TLeft, TRight>(
    val leftSide: Expression<TRoot, TLeft>,
    val rightSide: Expression<TRoot, TRight>
): Expression<TRoot, Boolean> {
    override fun asText(): String {
        return "${leftSide.asText()} == ${rightSide.asText()}"
    }

    override fun toString(): String {
        return asText()
    }
}