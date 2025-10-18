package de.fluxflow.flowquery.query.sorting

import de.fluxflow.flowquery.expression.Expression

data class Sort(
    val expression: Expression<*, *>,
    val direction: SortDirection,
) {
    
    fun toText(): String {
        return "${expression.toText()} ${direction.toText()}"
    }

    override fun toString(): String {
        return toText()
    }
    
    companion object {
        fun <TRoot, TCriteria> Expression<TRoot, TCriteria>.desc(): Sorting {
            return Sorting(
                Sort(
                    expression = this,
                    direction = SortDirection.Descending
                )
            )
        }

        fun <TRoot, TCriteria> Expression<TRoot, TCriteria>.asc(): Sorting {
            return Sorting(
                Sort(
                    expression = this,
                    direction = SortDirection.Ascending
                )
            )
        }
        
        fun <TRoot, TCriteria> Expression<TRoot, TCriteria>.sort(direction: SortDirection): Sorting {
            return Sorting(
                Sort(
                    expression = this,
                    direction = direction
                )
            )
        }
    }
}

