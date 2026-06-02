package de.lise.fluxflow.mongo.flowquery.repository

import de.fluxflow.flowquery.query.FlowQuery
import de.fluxflow.flowquery.query.QueryExecutionException
import de.fluxflow.flowquery.repository.FlowQueryRepository
import de.lise.fluxflow.query.pagination.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.aggregation.Aggregation
import kotlin.reflect.KClass

/**
 * MongoDB implementation of [FlowQueryRepository] using a generic [MongoExecutor].
 *
 * This class translates a [FlowQuery] into a Mongo aggregation pipeline via
 * [MongoQueryTranslator] and delegates execution to [MongoExecutor].
 *
 * It supports filtering, sorting, projection, pagination, and standard query operations
 * such as `find`, `findFirst`, and `findSingle`.
 */
class MongoFlowQueryRepository<TRoot : Any> internal constructor(
    private val rootType: Class<TRoot>,
    private val translator: MongoQueryTranslator,
    private val executor: MongoExecutor<TRoot>,
) : FlowQueryRepository<TRoot> {

    internal constructor(
        rootType: KClass<TRoot>,
        translator: MongoQueryTranslator,
        executor: MongoExecutor<TRoot>,
    ) : this(rootType.java, translator, executor)

    // --------------------------------------------------------------------------------------------
    // Find methods
    // --------------------------------------------------------------------------------------------

    override fun <TResult> find(resultType: Class<TResult>, query: FlowQuery<TRoot, TResult>): List<TResult> {
        return castElements(
            elements = execute(query, resultType).elements,
            resultType = resultType,
        )
    }

    override fun find(query: FlowQuery<TRoot, TRoot>): Page<TRoot> {
        val results = execute(query, rootType).page
        return Page(
            items = castElements(results.content, rootType),
            pageSize = query.pagination?.pageSize ?: results.totalElements.toInt(),
            pageIndex = query.pagination?.pageIndex ?: 0,
            totalPages = results.totalPages,
            totalItems = results.totalElements,
            isFirstPage = results.isFirst,
            isLastPage = results.isLast
        )
    }

    override fun <TResult> findFirst(resultType: Class<TResult>, query: FlowQuery<TRoot, TResult>): TResult {
        val first = execute(query.limit(1), resultType).elements.firstOrNull()
            ?: throw QueryExecutionException("Expected at least one result but found none.")

        return castElement(first, resultType)
    }

    override fun findFirst(query: FlowQuery<TRoot, TRoot>): TRoot {
        return findFirst(rootType, query)
    }

    override fun <TResult> findFirstOrNull(resultType: Class<TResult>, query: FlowQuery<TRoot, TResult>): TResult? {
        val first = execute(query.limit(1), resultType).elements.firstOrNull() ?: return null
        return castElement(first, resultType)
    }

    override fun findFirstOrNull(query: FlowQuery<TRoot, TRoot>): TRoot? {
        return findFirstOrNull(rootType, query)
    }

    override fun <TResult> findSingle(resultType: Class<TResult>, query: FlowQuery<TRoot, TResult>): TResult {
        val single = execute(query.limit(2), resultType).elements.singleOrNull()
            ?: throw QueryExecutionException("Expected exactly one result but found none or multiple.")

        return castElement(single, resultType)
    }

    override fun findSingle(query: FlowQuery<TRoot, TRoot>): TRoot {
        return findSingle(rootType, query)
    }

    override fun <TResult> findSingleOrNull(resultType: Class<TResult>, query: FlowQuery<TRoot, TResult>): TResult? {
        val single = execute(query.limit(2), resultType).elements.singleOrNull() ?: return null
        return castElement(single, resultType)
    }

    override fun findSingleOrNull(query: FlowQuery<TRoot, TRoot>): TRoot? {
        return findSingleOrNull(rootType, query)
    }

    // --------------------------------------------------------------------------------------------
    // Internal execution pipeline
    // --------------------------------------------------------------------------------------------

    private fun <TResult> execute(
        query: FlowQuery<TRoot, TResult>,
        resultType: Class<TResult>
    ): MongoExecutionResults<Any> {
        validateResultType(resultType)
        val mappedResultType = resultType.toMappedResultType()

        val pagination = query.pagination
        val pageRequest = pagination?.let { PageRequest.of(it.pageIndex, it.pageSize) }

        // Shortcut: plain collection scan without operations
        if (query.operations.isEmpty()) {
            return if (pageRequest == null) {
                UnpagedMongoResults(executor.findAll(mappedResultType))
            } else {
                executor.findPaged(pageRequest, mappedResultType)
            }
        }

        val aggregation = translator.translate(query)
        return if (pageRequest == null) {
            UnpagedMongoResults(executor.executeUnpaged(aggregation, mappedResultType))
        } else {
            executePaged(
                aggregation,
                pageRequest,
                pagination.pageIndex,
                pagination.pageSize,
                mappedResultType,
            )
        }
    }

    private fun <TResult : Any> executePaged(
        aggregation: Aggregation,
        pageRequest: PageRequest,
        pageIndex: Int,
        pageSize: Int,
        resultType: Class<TResult>,
    ): MongoExecutionResults<TResult> {
        val skip = Aggregation.skip(pageIndex.toLong() * pageSize.toLong())
        val limit = Aggregation.limit(pageSize.toLong())

        val pagedAggregation = Aggregation.newAggregation(aggregation.pipeline.operations + skip + limit)
        val countAggregation = Aggregation.newAggregation(
            aggregation.pipeline.operations + Aggregation.count().`as`(MongoCountResult::totalElements.name)
        )

        return executor.executePaged(pagedAggregation, countAggregation, pageRequest, resultType)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <TResult> Class<TResult>.toMappedResultType(): Class<Any> = this as Class<Any>

    private fun <TResult> castElements(elements: Iterable<Any>, resultType: Class<TResult>): List<TResult> {
        return elements.map { castElement(it, resultType) }
    }

    private fun <TResult> castElement(value: Any, resultType: Class<TResult>): TResult {
        if (!resultType.isInstance(value)) {
            throw QueryExecutionException(
                "Result mapping type mismatch: expected ${resultType.canonicalName}, got ${value::class.java.canonicalName}"
            )
        }

        @Suppress("UNCHECKED_CAST")
        return value as TResult
    }

    private fun validateResultType(resultType: Class<*>) {
        if (resultType.isPrimitive ||
            resultType.isArray ||
            String::class.java.isAssignableFrom(resultType) ||
            Collection::class.java.isAssignableFrom(resultType)
        ) {
            throw QueryExecutionException(
                "Result type must be a document/object type, not ${resultType.canonicalName}"
            )
        }
    }
}
