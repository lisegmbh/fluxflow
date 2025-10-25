package de.lise.fluxflow.mongo.flowquery.repository

import de.fluxflow.flowquery.query.FlowQuery
import de.fluxflow.flowquery.query.QueryExecutionException
import de.fluxflow.flowquery.repository.FlowQueryRepository
import de.lise.fluxflow.query.pagination.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.support.PageableExecutionUtils
import kotlin.reflect.KClass

/**
 * MongoDB implementation of [FlowQueryRepository] using the Spring Data [MongoTemplate].
 *
 * Delegates translation of [FlowQuery] objects to [MongoQueryTranslator]
 * and handles query execution, pagination, and result mapping.
 */
class MongoFlowQueryRepository<TRoot : Any> internal constructor(
    private val rootType: Class<TRoot>,
    private val translator: MongoQueryTranslator,
    private val mongoTemplate: MongoTemplate,
) : FlowQueryRepository<TRoot> {


    internal constructor(
        rootType: KClass<TRoot>,
        translator: MongoQueryTranslator,
        template: MongoTemplate,
    ) : this(rootType.java, translator, template)

    override fun <TResult> find(resultType: Class<TResult>, query: FlowQuery<TRoot, TResult>): List<TResult> {
        return execute(query, resultType).elements.toList()
    }

    override fun find(query: FlowQuery<TRoot, TRoot>): Page<TRoot> {
        val results = execute(query, rootType).page
        return Page(
            items = results.content,
            pageSize = query.pagination?.pageSize ?: results.totalElements.toInt(),
            pageIndex = query.pagination?.pageIndex ?: 0,
            totalPages = results.totalPages,
            totalItems = results.totalElements,
            isFirstPage = results.isFirst,
            isLastPage = results.isLast
        )
    }

    override fun <TResult> findFirst(resultType: Class<TResult>, query: FlowQuery<TRoot, TResult>): TResult {
        return execute(query.limit(1), resultType).elements.first()!!
    }

    override fun findFirst(query: FlowQuery<TRoot, TRoot>): TRoot {
        return findFirst(rootType, query)
    }

    override fun <TResult> findFirstOrNull(resultType: Class<TResult>, query: FlowQuery<TRoot, TResult>): TResult? {
        return execute(query.limit(1), resultType).elements.firstOrNull()
    }

    override fun findFirstOrNull(query: FlowQuery<TRoot, TRoot>): TRoot? {
        return findFirstOrNull(rootType, query)
    }

    override fun <TResult> findSingle(resultType: Class<TResult>, query: FlowQuery<TRoot, TResult>): TResult {
        return execute(query.limit(2), resultType).elements.single()!!
    }

    override fun findSingle(query: FlowQuery<TRoot, TRoot>): TRoot {
        return findSingle(rootType, query)
    }

    override fun <TResult> findSingleOrNull(resultType: Class<TResult>, query: FlowQuery<TRoot, TResult>): TResult? {
        return execute(query.limit(2), resultType).elements.singleOrNull()
    }

    override fun findSingleOrNull(query: FlowQuery<TRoot, TRoot>): TRoot? {
        return findSingleOrNull(rootType, query)
    }

    // --------------------------------------------------------------------------------------------
    // Internal execution pipeline
    // --------------------------------------------------------------------------------------------

    private fun <TResult> execute(query: FlowQuery<TRoot, TResult>, resultType: Class<TResult>): MongoExecutionResults<TResult> {
        validateResultType(resultType)

        val pagination = query.pagination
        val pageRequest = pagination?.let { PageRequest.of(it.pageIndex, it.pageSize) }

        // Shortcut: plain collection scan without operations
        if (query.operations.isEmpty()) {
            return when (pageRequest) {
                null -> UnpagedMongoResults(mongoTemplate.findAll(resultType) as List<TResult>)
                else -> PagedMongoResults(
                    PageableExecutionUtils.getPage(
                        mongoTemplate.find(
                            org.springframework.data.mongodb.core.query.Query().with(pageRequest),
                            rootType
                        ) as List<TResult>,
                        pageRequest
                    ) {
                        mongoTemplate.count(org.springframework.data.mongodb.core.query.Query(), rootType)
                    }
                )
            }
        }

        val aggregation = translator.translate(query)

        return if (pageRequest == null) {
            UnpagedMongoResults(
                mongoTemplate.aggregate(aggregation, rootType, resultType).mappedResults as List<TResult>
            )
        } else {
            executePaged(aggregation, pageRequest, pagination!!.pageIndex, pagination.pageSize, resultType)
        }
    }

    private fun <TResult> executePaged(
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

        val results = mongoTemplate.aggregate(pagedAggregation, rootType, resultType).mappedResults as List<TResult>
        return PagedMongoResults(
            PageableExecutionUtils.getPage(results, pageRequest) {
                mongoTemplate.aggregate(countAggregation, rootType, MongoCountResult::class.java)
                    .uniqueMappedResult?.totalElements ?: 0
            }
        )
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