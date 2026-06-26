package de.fluxflow.flowquery.expression

import kotlin.reflect.KType
import kotlin.reflect.typeOf

data class ContainsElementExpression<TRoot, TCollection : Collection<TElement>, TElement>(
    val collection: Expression<TRoot, TCollection>,
    val element: Expression<TRoot, TElement>
) : PredicateExpression<TRoot> {
    override val resultType: KType = typeOf<Boolean>()

    override fun toText(): String {
        return "${collection.toText()} CONTAINS ${element.toText()}"
    }
}