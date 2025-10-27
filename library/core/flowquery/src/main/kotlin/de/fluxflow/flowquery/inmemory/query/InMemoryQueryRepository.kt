package de.fluxflow.flowquery.inmemory.query

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.PredicateExpression
import de.fluxflow.flowquery.inmemory.expression.compilation.InMemoryCompiler
import de.fluxflow.flowquery.inmemory.query.sorting.InMemoryComparator
import de.fluxflow.flowquery.query.*
import de.fluxflow.flowquery.query.sorting.SortDirection
import de.fluxflow.flowquery.repository.FlowQueryRepository
import de.lise.fluxflow.query.pagination.Page

class InMemoryQueryRepository<TRoot>(
    private val compiler: InMemoryCompiler,
    private val elementGetter: () -> Collection<TRoot>
) : FlowQueryRepository<TRoot> {
    override fun <TResult> find(
        resultType: Class<TResult>,
        query: FlowQuery<TRoot, TResult>
    ): List<TResult> {
        var currentTransform: () -> Collection<Any?> = { elementGetter() }

        for (currentOperation in query.operations) {
            currentTransform = attachOperation(
                currentTransform,
                currentOperation
            )
        }

        return currentTransform().map {
            @Suppress("UNCHECKED_CAST")
            it as TResult
        }.toList()
    }

    override fun find(query: FlowQuery<TRoot, TRoot>): Page<TRoot> {
        var currentTransform: () -> Collection<Any?> = { elementGetter() }

        for (currentOperation in query.operations) {
            currentTransform = attachOperation(
                currentTransform,
                currentOperation
            )
        }

        val allResults = currentTransform().map {
            @Suppress("UNCHECKED_CAST")
            it as TRoot
        }.toList()

        return Page.fromResult(allResults, query.pagination)
    }

    override fun <TResult> findFirst(
        resultType: Class<TResult>,
        query: FlowQuery<TRoot, TResult>
    ): TResult {
        return find(
            resultType,
            query
        ).first()
    }

    override fun findFirst(query: FlowQuery<TRoot, TRoot>): TRoot {
        return find(query).items.first()
    }

    override fun <TResult> findFirstOrNull(
        resultType: Class<TResult>,
        query: FlowQuery<TRoot, TResult>
    ): TResult? {
        return find(
            resultType,
            query
        ).firstOrNull()
    }

    override fun findFirstOrNull(query: FlowQuery<TRoot, TRoot>): TRoot? {
        return find(query).items.firstOrNull()
    }

    override fun <TResult> findSingle(
        resultType: Class<TResult>,
        query: FlowQuery<TRoot, TResult>
    ): TResult {
        return find(
            resultType,
            query
        ).single()
    }

    override fun findSingle(query: FlowQuery<TRoot, TRoot>): TRoot {
        return find(query).items.single()
    }

    override fun <TResult> findSingleOrNull(
        resultType: Class<TResult>,
        query: FlowQuery<TRoot, TResult>
    ): TResult? {
        return find(
            resultType,
            query
        ).singleOrNull()
    }

    override fun findSingleOrNull(query: FlowQuery<TRoot, TRoot>): TRoot? {
        return find(query).items.singleOrNull()
    }

    private fun attachOperation(
        currentTransform: () -> Collection<Any?>,
        operation: QueryOperation
    ): () -> Collection<Any?> {
        return when (operation) {
            is FilterOperation -> {
                val predicate = operation.predicate as PredicateExpression<Any?>
                val compiled = compiler.compile(predicate).result

                return {
                    currentTransform().filter {
                        compiled.execute(it) ?: false
                    }
                }
            }

            is ProjectionOperation -> {
                val predicate = operation.projection as Expression<Any?, Any?>
                val compiled = compiler.compile(predicate).result

                return {
                    currentTransform().map {
                        compiled.execute(it)
                    }
                }
            }

            is LimitOperation -> {
                return {
                    currentTransform().take(operation.amount.toInt())
                }
            }

            is SortingOperation -> {
                val sortCriteria = operation.sorting
                val combinedComparator = sortCriteria.sorts.map { sort ->
                    val sortExpression = sort.expression as Expression<Any?, Any?>
                    val compiled = compiler.compile(sortExpression).result
                    val comparator = Comparator.comparing<Any?, Any?>(
                        { compiled.execute(it) },
                        InMemoryComparator()
                    )
                    when(sort.direction) {
                        SortDirection.Ascending -> comparator
                        SortDirection.Descending -> comparator.reversed()
                    }
                }.reduce { a, b ->
                    a.then(b)
                }
                return {
                    currentTransform().sortedWith(combinedComparator)
                }
            }
        }
    }
}