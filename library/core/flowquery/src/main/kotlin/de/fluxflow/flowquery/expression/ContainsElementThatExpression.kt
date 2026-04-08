package de.fluxflow.flowquery.expression

data class ContainsElementThatExpression<TRoot, TCollection : Collection<TElement>, TElement>(
    val collection: Expression<TRoot, TCollection>,
    val elementPredicate: PredicateExpression<TElement>
) : LogicalExpression<TRoot> {
    override fun toText(): String {
        return "${collection.toText()} CONTAINS ELEMENT THAT (${elementPredicate.toText()})"
    }

    override fun toString(): String {
        return toText()
    }
}