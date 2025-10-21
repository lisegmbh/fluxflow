package de.fluxflow.flowquery.expression

data class ContainsElementExpression<TRoot, TCollection : Collection<TElement>, TElement>(
    val collection: Expression<TRoot, TCollection>,
    val element: Expression<TRoot, TElement>
) : PredicateExpression<TRoot> {
    override fun toText(): String {
        return "${collection.toText()} CONTAINS ${element.toText()}"
    }
}