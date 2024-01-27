package de.lise.fluxflow.engine.reflection

typealias JavaEqualityChecker<TElement> = EqualityChecker<TElement, Any?>

fun interface EqualityChecker<in TElement, in TOther> {
    fun checkIfEqual(instance: TElement, other: TOther): Boolean

    infix fun <TNewElement : TElement, TNewOther: TOther> and(
        checker: EqualityChecker<TNewElement, TNewOther>
    ): EqualityChecker<TNewElement, TNewOther> {
        return EqualityChecker { instance, other ->
            checkIfEqual(instance, other) && checker.checkIfEqual(instance, other)
        }
    }

    companion object {
        fun <TElement, TOther> alwaysEqual(): EqualityChecker<TElement, TOther> {
            return EqualityChecker { _, _ ->
                true
            }
        }
    }
}