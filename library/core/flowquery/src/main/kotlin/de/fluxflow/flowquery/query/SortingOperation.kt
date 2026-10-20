package de.fluxflow.flowquery.query

import de.fluxflow.flowquery.query.sorting.Sorting

data class SortingOperation(
    val sorting: Sorting
): QueryOperation {

    override fun toText(): String {
        return sorting.toText()
    }

    override fun toString(): String {
        return toText()
    }
}