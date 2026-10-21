package de.lise.fluxflow.mongo.flowquery.repository

import de.fluxflow.flowquery.query.FlowQuery
import de.fluxflow.flowquery.query.QueryExecutionException
import de.fluxflow.flowquery.query.sorting.Sort.Companion.asc
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query

/**
 * Regression contract for `findFirst`/`findSingle` on paged queries, shared by both Spring Boot lines.
 *
 * The documents are inserted out of order, so every expectation below depends on the explicit sorting
 * by [PagedDocument.name] and the page selected from it.
 */
abstract class AbstractMongoPagedFirstSingleIT {
    @Autowired
    private lateinit var template: MongoTemplate

    @Autowired
    internal lateinit var translator: MongoQueryTranslator

    private val repo: MongoFlowQueryRepository<PagedDocument> by lazy {
        MongoFlowQueryRepository(
            rootType = PagedDocument::class.java,
            translator = translator,
            executor = MongoExecutor(template, PagedDocument::class.java),
        )
    }

    @BeforeEach
    fun setupDocuments() {
        template.findAllAndRemove(Query(), PagedDocument::class.java)
        template.insertAll(listOf("C", "A", "E", "B", "D").map { PagedDocument(it, it, PagedDetails(it.lowercase())) })
    }

    @Test
    fun `first and single should evaluate a later page with multiple elements`() {
        assertFirstAndSingle(sortedByName().paged(1, 2), expectedFirst = "C", expectedSingle = null)
    }

    @Test
    fun `first and single should evaluate a partial last page`() {
        assertFirstAndSingle(sortedByName().paged(2, 2), expectedFirst = "E", expectedSingle = "E")
    }

    @Test
    fun `first and single should evaluate a later page with a single element`() {
        assertFirstAndSingle(sortedByName().paged(4, 1), expectedFirst = "E", expectedSingle = "E")
    }

    @Test
    fun `first and single should report an empty page`() {
        assertFirstAndSingle(sortedByName().paged(5, 1), expectedFirst = null, expectedSingle = null)
    }

    @Test
    fun `first and single should evaluate the first page`() {
        assertFirstAndSingle(sortedByName().paged(0, 2), expectedFirst = "A", expectedSingle = null)
    }

    @Test
    fun `first and single should keep a caller limit when paging`() {
        // The caller limit leaves [A, B, C], so the second page of size 2 is [C].
        assertFirstAndSingle(sortedByName().limit(3).paged(1, 2), expectedFirst = "C", expectedSingle = "C")
    }

    @Test
    fun `first and single should keep their semantics without pagination`() {
        assertFirstAndSingle(sortedByName(), expectedFirst = "A", expectedSingle = null)
        assertFirstAndSingle(
            FlowQuery.of<PagedDocument>().where { get(PagedDocument::name).isEqual("B") },
            expectedFirst = "B",
            expectedSingle = "B",
        )
    }

    @Test
    fun `first should evaluate a later page of a projection`() {
        val query = sortedByName().project { get(PagedDocument::details) }.paged(1, 2)

        assertThat(repo.findFirst(PagedDetails::class, query).label).isEqualTo("c")
        assertThat(repo.findFirstOrNull(PagedDetails::class, query)?.label).isEqualTo("c")
    }

    @Test
    fun `first and single should support queries without operations`() {
        // Without sorting the natural order is unspecified, so only membership is asserted.
        val names = listOf("A", "B", "C", "D", "E")
        val unpaged = FlowQuery.of<PagedDocument>()
        val laterPage = unpaged.paged(1, 2)

        assertThat(repo.findFirst(unpaged).name).isIn(names)
        assertThat(repo.findSingleOrNull(unpaged)).isNull()
        assertThat(repo.findFirst(laterPage).name).isIn(names)
        assertThat(repo.findFirstOrNull(laterPage)?.name).isIn(names)
        assertThat(repo.findSingleOrNull(laterPage)).isNull()
    }

    private fun sortedByName(): FlowQuery<PagedDocument, PagedDocument> {
        return FlowQuery.of<PagedDocument>().sort { get(PagedDocument::name).asc() }
    }

    private fun assertFirstAndSingle(
        query: FlowQuery<PagedDocument, PagedDocument>,
        expectedFirst: String?,
        expectedSingle: String?,
    ) {
        assertThat(repo.findFirstOrNull(query)?.name).isEqualTo(expectedFirst)
        assertThat(repo.findSingleOrNull(query)?.name).isEqualTo(expectedSingle)

        if (expectedFirst == null) {
            assertThatThrownBy { repo.findFirst(query) }.isInstanceOf(QueryExecutionException::class.java)
        } else {
            assertThat(repo.findFirst(query).name).isEqualTo(expectedFirst)
        }

        if (expectedSingle == null) {
            assertThatThrownBy { repo.findSingle(query) }.isInstanceOf(QueryExecutionException::class.java)
        } else {
            assertThat(repo.findSingle(query).name).isEqualTo(expectedSingle)
        }
    }

    data class PagedDocument(
        val id: String,
        val name: String,
        val details: PagedDetails,
    )

    data class PagedDetails(val label: String)
}
