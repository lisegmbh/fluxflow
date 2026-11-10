package de.lise.fluxflow.mongo.flowquery.repository

import de.fluxflow.flowquery.query.FlowQuery
import de.fluxflow.flowquery.query.QueryConstraint
import de.fluxflow.flowquery.query.sorting.Sort.Companion.asc
import de.lise.fluxflow.mongo.FluxFlowMongoQueryPipelineProvider
import de.lise.fluxflow.mongo.MongoIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Query

@MongoIntegrationTest
class MongoQueryPipelineIT {
    private object Restriction : QueryConstraint
    @Autowired lateinit var template: MongoTemplate
    @Autowired internal lateinit var translator: MongoQueryTranslator

    @BeforeEach
    fun insertFixtures() {
        template.findAllAndRemove(Query(), QueryDocument::class.java)
        template.insertAll((0..5).map { QueryDocument(it.toString(), it % 2 == 1, View(it.toString())) })
    }

    @Test
    fun `permission prefix determines page contents and complete counts in real Mongo`() {
        val repo = repository()
        val pages = (0..2).map { index -> repo.find { sort { get(QueryDocument::id).asc() }.paged(index, 2) } }
        assertThat(pages.flatMap { it.items }.map { it.id }).containsExactly("1", "3", "5")
        assertThat(pages.map { it.totalItems }).containsOnly(3L)
        assertThat(pages.map { it.totalPages }).containsOnly(2)
        assertThat(pages.map { it.pageSize }).containsOnly(2)
        assertThat(pages.map { it.isLastPage }).containsExactly(false, true, true)
    }

    @Test
    fun `projection and explicit cap cannot remove the raw permission predicate`() {
        val repo = repository()
        val projected = repo.find(View::class.java, FlowQuery.of<QueryDocument>()
            .sort { get(QueryDocument::id).asc() }
            .project { get(QueryDocument::view) }.limit(1))
        assertThat(projected.map { it.id }).containsExactly("1")
        assertThat(repo.findFirst(FlowQuery.of<QueryDocument>()).id).isEqualTo("1")
    }

    @Test
    fun `explicit constrained views leave ordinary query results unchanged`() {
        val ordinary = MongoFlowQueryRepository(QueryDocument::class.java, translator,
            MongoExecutor(template, QueryDocument::class.java), listOf(FluxFlowMongoQueryPipelineProvider { _, _ ->
                listOf(Aggregation.stage(Document("\$match", Document("readable", true))))
            }))
        assertThat(ordinary.withConstraint(Restriction).find(FlowQuery.of<QueryDocument>()).items).hasSize(3)
        assertThat(ordinary.find(FlowQuery.of<QueryDocument>()).items).hasSize(6)
    }

    private fun repository(provider: FluxFlowMongoQueryPipelineProvider = FluxFlowMongoQueryPipelineProvider { _, _ ->
        listOf(Aggregation.stage(Document("\$match", Document("readable", true))))
    }) = MongoFlowQueryRepository(QueryDocument::class.java, translator,
        MongoExecutor(template, QueryDocument::class.java), listOf(provider)).withConstraint(Restriction)

    @org.springframework.data.mongodb.core.mapping.Document("queryPipelineFixtures")
    data class QueryDocument(val id: String, val readable: Boolean, val view: View)
    data class View(val id: String)
}
