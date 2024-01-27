package de.lise.fluxflow.engine.reflection

fun interface HashComposer<in TElement> {
    fun composeHashCode(element: TElement): Int

    infix fun <TOther : TElement> and(other: HashComposer<TOther>): HashComposer<TOther> {
        return HashComposer { instance ->
            composeHashCode(instance) xor other.composeHashCode(instance)
        }
    }
}