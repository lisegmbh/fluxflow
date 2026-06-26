package de.fluxflow.flowquery.expression

import kotlin.reflect.KType
import kotlin.reflect.typeOf

class HasKeyExpression<TRoot, TKey>(
    val instance: Expression<TRoot, out Map<TKey, *>>,
    val key: Expression<TRoot, TKey>
) : Expression<TRoot, Boolean> {
    override val resultType: KType = typeOf<Boolean>()

    override fun toText(): String {
        return "${instance.toText()} HAS PROPERTY ${key.toText()}"
    }
}