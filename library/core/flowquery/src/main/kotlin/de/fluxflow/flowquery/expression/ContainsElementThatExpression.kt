package de.fluxflow.flowquery.expression

import kotlin.reflect.KType
import kotlin.reflect.typeOf

data class ContainsElementThatExpression<TRoot, TCollection : Collection<TElement>, TElement>(
    val collection: Expression<TRoot, TCollection>,
    val elementPredicate: PredicateExpression<TElement>
) : PredicateExpression<TRoot> {
    override val resultType: KType = typeOf<Boolean>()

    override fun toText(): String {
        return "${collection.toText()} CONTAINS ELEMENT THAT (${elementPredicate.toText()})"
    }

    override fun toString(): String {
        return toText()
    }
}