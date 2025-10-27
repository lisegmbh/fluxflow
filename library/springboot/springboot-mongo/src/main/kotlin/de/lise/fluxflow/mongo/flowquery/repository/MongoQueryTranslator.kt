package de.lise.fluxflow.mongo.flowquery.repository

import de.fluxflow.flowquery.query.*
import de.fluxflow.flowquery.query.sorting.SortDirection
import de.lise.fluxflow.mongo.flowquery.expression.compilation.MongoCompiler
import de.lise.fluxflow.mongo.flowquery.expression.compilation.token.*
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import kotlin.reflect.typeOf

/**
 * Translates a [FlowQuery] into a MongoDB [Aggregation] pipeline.
 *
 * This class encapsulates all logic for mapping query operations such as filtering,
 * projection, sorting, and limiting into their Mongo aggregation counterparts.
 *
 * It does **not** execute queries — only builds the aggregation model that
 * [MongoFlowQueryRepository] can later execute.
 */
internal class MongoQueryTranslator(private val compiler: MongoCompiler) {

    private val logger = LoggerFactory.getLogger(MongoQueryTranslator::class.java)

    /**
     * Translates the given [query] into a MongoDB [Aggregation] instance.
     *
     * @param query the [FlowQuery] to translate
     * @return the resulting [Aggregation] pipeline
     */
    fun <TRoot, TResult> translate(query: FlowQuery<TRoot, TResult>): Aggregation {
        val operations = query.operations.flatMap { toAggregationOperation(it) }
        val aggregation = Aggregation.newAggregation(operations)

        if (logger.isTraceEnabled) {
            logger.trace(
                "Compiled FlowQuery to MongoDB aggregation pipeline: Original query:\n{}\n-> Result query:\n{}",
                query.toText(),
                aggregation.pipeline
            )
        }

        return aggregation
    }

    /**
     * Converts a single [QueryOperation] into one or more Mongo [AggregationOperation]s.
     */
    private fun toAggregationOperation(operation: QueryOperation): List<AggregationOperation> {
        return when (operation) {
            is FilterOperation -> compiler.compile(operation.predicate).result.let {
                listOf(toMatch(operation, it))
            }

            is ProjectionOperation -> compiler.compile(operation.projection).result.let {
                toProjection(operation, it)
            }

            is SortingOperation -> toSorting(operation)

            is LimitOperation -> listOf(Aggregation.limit(operation.amount))
        }
    }

    /**
     * Translates a [FilterOperation] into a Mongo `$match` stage.
     */
    private fun toMatch(operation: FilterOperation, token: MongoToken): AggregationOperation {
        return when (token) {
            is ExpressionToken -> MatchToken(token).toStage()
            is MatchToken -> token.toStage()
            is PropertyToken if token.property.returnType in listOf(typeOf<Boolean?>(), typeOf<Boolean>()) -> {
                MatchToken(
                    StatementOperationToken(token, "eq", ConstantToken(true))
                ).toStage()
            }

            else -> throw QueryExecutionException(
                "Cannot filter by '${operation.toText()}': expected expression or property, got ${token::class.simpleName}"
            )
        }
    }

    /**
     * Translates a [ProjectionOperation] into Mongo `$project` and `$replaceRoot` stages.
     */
    private fun toProjection(operation: ProjectionOperation, token: MongoToken): List<AggregationOperation> {
        return when (token) {
            is StatementToken -> listOf(
                ProjectToken("result", token).toStage(),
                Aggregation.replaceRoot("result")
            )

            else -> throw QueryExecutionException(
                "Cannot project by '${operation.toText()}': expected statement, got ${token::class.simpleName}"
            )
        }
    }

    /**
     * Translates a [SortingOperation] into a Mongo `$sort` stage.
     */
    private fun toSorting(operation: SortingOperation): List<AggregationOperation> {
        val sortSpec = operation.sorting.sorts.associate { sort ->
            val token = compiler.compile(sort.expression).result
            val sortKey = toSortKey(operation, token)
            sortKey.toStatement() to when (sort.direction) {
                SortDirection.Ascending -> 1
                SortDirection.Descending -> -1
            }
        }

        return listOf(
            Aggregation.stage(
                Document(mapOf("\$sort" to Document(sortSpec)))
            )
        )
    }

    private fun toSortKey(operation: SortingOperation, token: MongoToken): StatementToken {
        return when (token) {
            is StatementToken -> token
            else -> throw QueryExecutionException(
                "Cannot sort by '${operation.toText()}': expected statement, got ${token::class.simpleName}"
            )
        }
    }
}
