package de.fluxflow.flowquery.expression

class IsEqual<TRoot, TLeft, TRight>(
    val leftSide: Expression<TRoot, TLeft>,
    val rightSide: Expression<TRoot, TRight>
): Expression<TRoot, Boolean> {
    override fun toText(): String {
        return "${leftSide.toText()} == ${rightSide.toText()}"
    }

    override fun toString(): String {
        return toText()
    }
}