package de.fluxflow.flowquery.query.sorting

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.query.sorting.Sort.Companion.asc
import de.fluxflow.flowquery.query.sorting.Sort.Companion.desc

data class Sorting(
    val sorts: List<Sort>,
) {
    constructor(vararg sorts: Sort) : this(
        listOf(*sorts)
    )
    
    fun thenBy(
        vararg sorts: Sort
    ): Sorting {
        return thenBy(listOf(*sorts))
    }
    
    fun thenBy(
        sorts: List<Sort>
    ): Sorting {
        return copy(
            sorts = this.sorts + sorts
        )
    }
    
    fun thenBy(
        sorting: Sorting
    ): Sorting {
        return copy(
            sorts = this.sorts + sorting.sorts
        )
    }
    
    fun thenByAll(
        sorting: List<Sorting>
    ): Sorting {
        return thenBy(
            sorting.flatMap { it.sorts }
        )
    }

    fun thenByDesc(
        expressions: List<Expression<*, *>>
    ): Sorting {
        return thenByAll(
            expressions.map { it.desc() }
        )
    }
    
    fun thenByDesc(
        vararg expressions: Expression<*, *>
    ): Sorting {
        return thenByDesc(
            listOf(*expressions)    
        )
    }
    
    fun thenByAsc(
        expressions: List<Expression<*, *>>
    ): Sorting {
        return thenByAll(
            expressions.map { it.asc() }
        )
    }
    
    fun thenByAsc(
        vararg expressions: Expression<*, *>
    ): Sorting {
        return thenByAsc(listOf(*expressions))
    }
    
    inline fun <reified TRoot> thenByAsc(
        vararg builder: (Expression<TRoot, *>).() -> Expression<TRoot, *>
    ): Sorting {
        return thenByAll(
            builder.map { 
                it(Expression.root()).asc()
            }
        )
    }

    fun toText(): String {
        return "SORT BY ${
            sorts.joinToString(", ") {
                it.toText()
            }
        }"
    }

    override fun toString(): String {
        return toText()
    }
}