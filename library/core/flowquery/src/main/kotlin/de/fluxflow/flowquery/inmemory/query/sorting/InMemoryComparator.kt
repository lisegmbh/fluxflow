package de.fluxflow.flowquery.inmemory.query.sorting

import de.lise.fluxflow.query.sort.IncomparableElementsException

class InMemoryComparator : Comparator<Any?> {
    override fun compare(a: Any?, b: Any?): Int {
        return when {
            a == null && b == null -> 0
            a == null -> -1
            b == null -> 1
            a is Comparable<*> && b is Comparable<*> -> {
                try {
                    (a as Comparable<Any?>).compareTo(b)
                } catch (e: Throwable) {
                    throw IncomparableElementsException(a, b, e)
                }
            }
            else -> throw IncomparableElementsException(
                "Could not compare '$a' to '$b', as they are not comparable to each other."
            )
        }
    }
}