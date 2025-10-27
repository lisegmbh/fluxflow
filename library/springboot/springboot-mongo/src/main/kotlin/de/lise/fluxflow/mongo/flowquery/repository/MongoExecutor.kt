package de.lise.fluxflow.mongo.flowquery.repository

import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.support.PageableExecutionUtils

internal class MongoExecutor<TRoot>(
    private val mongoTemplate: MongoTemplate,
    private val rootType: Class<TRoot>,
) {
    fun <TResult> executeUnpaged(aggregation: Aggregation, resultType: Class<TResult>): List<TResult> {
        return mongoTemplate.aggregate(aggregation, rootType, resultType).mappedResults
    }

    fun <TResult> executePaged(
        aggregation: Aggregation,
        countAggregation: Aggregation,
        pageRequest: PageRequest,
        resultType: Class<TResult>,
    ): PagedMongoResults<TResult> {
        val results = mongoTemplate.aggregate(aggregation, rootType, resultType).mappedResults
        val totalCount = mongoTemplate.aggregate(countAggregation, rootType, MongoCountResult::class.java)
            .uniqueMappedResult?.totalElements ?: 0

        return PagedMongoResults(
            PageableExecutionUtils.getPage(results, pageRequest) { totalCount }
        )
    }

    fun <TResult> findAll(resultType: Class<TResult>): List<TResult> {
        return mongoTemplate.findAll(resultType)
    }

    fun <TResult> findPaged(pageRequest: PageRequest, resultType: Class<TResult>): PagedMongoResults<TResult> {
        val query = org.springframework.data.mongodb.core.query.Query().with(pageRequest)
        val collectionName = mongoTemplate.getCollectionName(rootType)
        val results = mongoTemplate.find(
            query,
            resultType,
            collectionName
        )

        return PagedMongoResults(
            PageableExecutionUtils.getPage(results, pageRequest) {
                mongoTemplate.count(org.springframework.data.mongodb.core.query.Query(), collectionName)
            }
        )
    }
}
