package de.fluxflow.flowquery.inmemory.query

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.FlowPredicate
import de.fluxflow.flowquery.inmemory.InMemoryCompiler
import de.fluxflow.flowquery.query.*
import de.fluxflow.flowquery.repository.FlowQueryRepository

class InMemoryQueryRepository<TRoot>(
    private val compiler: InMemoryCompiler,
    private val elements: Collection<TRoot>
): FlowQueryRepository<TRoot> {
    override fun <TResult> find(
        resultType: Class<TResult>,
        query: Query<TRoot, TResult>
    ): List<TResult> {
        var currentTransform: () -> Collection<Any?> = { elements }

        for(currentOperation in query.operations) {
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

    override fun find(query: Query<TRoot, TRoot>): List<TRoot> {
        var currentTransform: () -> Collection<Any?> = { elements }

        for(currentOperation in query.operations) {
            currentTransform = attachOperation(
                currentTransform,
                currentOperation
            )
        }

        return currentTransform().map {
            @Suppress("UNCHECKED_CAST")
            it as TRoot
        }.toList()
    }

    override fun <TResult> findFirst(
        resultType: Class<TResult>,
        query: Query<TRoot, TResult>): TResult {
        return find(
            resultType,
            query
        ).first()
    }

    override fun findFirst(query: Query<TRoot, TRoot>): TRoot {
        return find(query).first()
    }

    override fun <TResult> findFirstOrNull(
        resultType: Class<TResult>,
        query: Query<TRoot, TResult>
    ): TResult? {
        return find(
            resultType,
            query
        ).firstOrNull()
    }

    override fun findFirstOrNull(query: Query<TRoot, TRoot>): TRoot? {
        return find(query).firstOrNull()
    }

    override fun <TResult> findSingle(
        resultType: Class<TResult>,
        query: Query<TRoot, TResult>
    ): TResult {
        return find(
            resultType,
            query
        ).single()
    }

    override fun findSingle(query: Query<TRoot, TRoot>): TRoot {
        return find(query).single()
    }

    override fun <TResult> findSingleOrNull(
        resultType: Class<TResult>,
        query: Query<TRoot, TResult>
    ): TResult? {
        return find(
            resultType,
            query
        ).singleOrNull()
    }

    override fun findSingleOrNull(query: Query<TRoot, TRoot>): TRoot? {
        return find(query).singleOrNull()
    }

    private fun attachOperation(
        currentTransform: () -> Collection<Any?>,
        operation: QueryOperation
    ): () -> Collection<Any?> {
        return when(operation) {
            is FilterOperation -> {
                val predicate = operation.predicated as FlowPredicate<Any?>
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
            else -> throw UnsupportedQueryOperationException(operation)
        }
    }
}
