package de.lise.fluxflow.engine.reflection

import kotlin.reflect.KClass

data class EqualsAndHashCode<in TElement>(
    val equalityChecker: JavaEqualityChecker<TElement>,
    val hashCodeComposer: HashComposer<TElement>
) {
    fun areEqual(instance: TElement, other: Any?): Boolean {
        return equalityChecker.checkIfEqual(instance, other)
    }

    fun composeHash(instance: TElement): Int {
        return hashCodeComposer.composeHashCode(instance)
    }

    companion object {
        @JvmStatic
        fun <TElement : Any> forType(
            type: KClass<TElement>,
            makeAccessible: Boolean = true
        ): EqualsAndHashCode<TElement> {
            return EqualsAndHashCodeBuilder(
                type,
                makeAccessible = makeAccessible
            ).build()
        }

        @JvmStatic
        inline fun <reified TElement : Any> forType(
            makeAccessible: Boolean = true
        ): EqualsAndHashCode<TElement> {
            return forType(TElement::class, makeAccessible)
        }
    }
}