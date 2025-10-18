package de.lise.fluxflow.mongo.flowquery.repository

import de.fluxflow.flowquery.query.*
import de.fluxflow.flowquery.query.sorting.SortDirection
import de.fluxflow.flowquery.repository.FlowQueryRepository
import de.lise.fluxflow.mongo.flowquery.MongoCompiler
import de.lise.fluxflow.mongo.flowquery.token.*
import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import kotlin.reflect.KClass

class MongoQueryRepository<TRoot : Any> internal constructor(
    private val rootType: Class<TRoot>,
    private val compiler: MongoCompiler,
    private val mongoTemplate: MongoTemplate,
) : FlowQueryRepository<TRoot> {
    internal constructor(
        rootType: KClass<TRoot>,
        compiler: MongoCompiler,
        template: MongoTemplate,
    ) : this(
        rootType = rootType.java,
        compiler = compiler,
        mongoTemplate = template
    )

    override fun <TResult> find(
        resultType: Class<TResult>,
        query: Query<TRoot, TResult>,
    ): List<TResult> {
        return execute(
            query,
            resultType
        ).toList() as List<TResult>
    }

    override fun find(query: Query<TRoot, TRoot>): List<TRoot> {
        return find(
            rootType,
            query
        )
    }

    override fun <TResult> findFirst(
        resultType: Class<TResult>,
        query: Query<TRoot, TResult>,
    ): TResult {
        return execute(
            query,
            resultType
        ).first()!!
    }

    override fun findFirst(query: Query<TRoot, TRoot>): TRoot {
        return findFirst(
            rootType,
            query
        )
    }

    override fun <TResult> findFirstOrNull(
        resultType: Class<TResult>,
        query: Query<TRoot, TResult>,
    ): TResult? {
        return execute(
            query,
            resultType
        ).firstOrNull()
    }

    override fun findFirstOrNull(query: Query<TRoot, TRoot>): TRoot? {
        return findFirstOrNull(
            rootType,
            query
        )
    }

    override fun <TResult> findSingle(
        resultType: Class<TResult>,
        query: Query<TRoot, TResult>,
    ): TResult {
        return execute(
            query,
            resultType
        ).single()!!
    }

    override fun findSingle(query: Query<TRoot, TRoot>): TRoot {
        return findSingle(
            rootType,
            query
        )
    }

    override fun <TResult> findSingleOrNull(
        resultType: Class<TResult>,
        query: Query<TRoot, TResult>,
    ): TResult? {
        return execute(
            query,
            resultType
        ).singleOrNull()
    }

    override fun findSingleOrNull(query: Query<TRoot, TRoot>): TRoot? {
        return findSingleOrNull(
            rootType,
            query
        )
    }

    private fun <TResult> execute(
        query: Query<TRoot, TResult>,
        resultType: Class<TResult>,
    ): Iterable<TResult?> {
        if (
            resultType.isPrimitive ||
            resultType.isArray ||
            String::class.java.isAssignableFrom(resultType) ||
            Collection::class.java.isAssignableFrom(resultType)
        ) {
            throw QueryExecutionException("Results must be a document/object type. The actual result type is: ${resultType.canonicalName}")
        }
        if(query.operations.isEmpty()) {
          return mongoTemplate.findAll(resultType)
        }

        val aggregation = toAggregation(query)
        return mongoTemplate.aggregate(
            aggregation,
            rootType,
            resultType
        )
    }

    private fun <TResult> toAggregation(
        query: Query<TRoot, TResult>,
    ): Aggregation {
        return query.operations.flatMap {
            toAggregationOperation(it)
        }.let {
            Aggregation.newAggregation(it)
        }
    }

    private fun toAggregationOperation(
        operation: QueryOperation,
    ): List<AggregationOperation> {
        return when (operation) {
            is FilterOperation -> {
                compiler.compile(operation.predicated).result.let {
                    listOf(
                        toMatch(
                            operation,
                            it
                        )
                    )
                }
            }

            is ProjectionOperation -> {
                compiler.compile(operation.projection).result.let {
                    toProjection(
                        operation,
                        it
                    )
                }
            }

            is SortingOperation -> {
                operation.sorting.sorts.associate { sort ->
                    toSortKey(
                        operation,
                        compiler.compile(sort.expression).result
                    ).toStatement() to when (sort.direction) {
                        SortDirection.Ascending -> 1
                        SortDirection.Descending -> -1
                    }
                }.let {
                    listOf(
                        Aggregation.stage(
                            Document(
                                mapOf(
                                    $$"$sort" to Document(it)
                                )
                            )
                        )
                    )
                }
            }

            else -> throw UnsupportedQueryOperationException(operation)
        }
    }

    private fun toSortKey(
        operation: SortingOperation,
        token: MongoToken,
    ): StatementToken {
        return when (token) {
            is StatementToken -> token
            else -> throw QueryExecutionException(
                "Can not sort by '${operation.toText()}', as a statement is expected (actual type is: ${token::class.simpleName})."
            )
        }
    }

    private fun toProjection(
        operation: ProjectionOperation,
        token: MongoToken,
    ): List<AggregationOperation> {
        return when (token) {
            is StatementToken -> listOf(
                ProjectToken(
                    "result",
                    token
                ).toStage(),
                Aggregation.replaceRoot("result")
            )

            else -> throw QueryExecutionException(
                "Can not project by '${operation.toText()}', as a statement is expected (actual type is: ${token::class.simpleName})."
            )
        }
    }

    private fun toMatch(
        operation: FilterOperation,
        token: MongoToken,
    ): AggregationOperation {

        return when (token) {
            is ExpressionToken -> MatchToken(token).toStage()
            is MatchToken -> token.toStage()
            else -> throw QueryExecutionException(
                "Can not filter by '${operation.toText()}', as an expression is expected (actual type is: ${token::class.simpleName})."
            )
        }
    }
}