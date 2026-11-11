package de.lise.fluxflow.mongo.flowquery.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationResults
import org.springframework.data.mongodb.core.query.Query

class MongoExecutorTest {
    private lateinit var mongoTemplate: MongoTemplate
    private lateinit var executor: MongoExecutor<TestRoot>

    private val rootType = TestRoot::class.java

    data class TestRoot(val id: String, val name: String)
    data class TestProjection(val name: String)

    @BeforeEach
    fun setup() {
        mongoTemplate = mock()
        executor = MongoExecutor(mongoTemplate, rootType)
    }

    // --------------------------------------------------------------------------------------------
    // executeUnpaged
    // --------------------------------------------------------------------------------------------

    @Test
    fun `executeUnpaged should delegate to MongoTemplate#aggregate and return mapped results`() {
        // Arrange
        val aggregation = mock<Aggregation>()
        val expectedResults = listOf(TestProjection("foo"), TestProjection("bar"))
        val aggregationResults: AggregationResults<TestProjection> = mock {
            on { mappedResults }.thenReturn(expectedResults)
        }

        whenever(mongoTemplate.aggregate(aggregation, rootType, TestProjection::class.java))
            .thenReturn(aggregationResults)

        // Act
        val results = executor.executeUnpaged(aggregation, TestProjection::class.java)

        // Assert
        assertThat(results).containsExactlyElementsOf(expectedResults)
        verify(mongoTemplate).aggregate(aggregation, rootType, TestProjection::class.java)
    }

    // --------------------------------------------------------------------------------------------
    // executePaged
    // --------------------------------------------------------------------------------------------

    @Test
    fun `executePaged should run aggregation and count returning a PagedMongoResults`() {
        // Arrange
        val aggregation = mock<Aggregation>()
        val countAggregation = mock<Aggregation>()
        val pageRequest = PageRequest.of(0, 2)
        val expectedResults = listOf(TestProjection("foo"), TestProjection("bar"))

        val aggregationResults: AggregationResults<TestProjection> = mock {
            on { mappedResults }.thenReturn(expectedResults)
        }
        val countResults: AggregationResults<MongoCountResult> = mock {
            on { uniqueMappedResult }.thenReturn(MongoCountResult(10))
        }

        whenever(mongoTemplate.aggregate(aggregation, rootType, TestProjection::class.java))
            .thenReturn(aggregationResults)
        whenever(mongoTemplate.aggregate(countAggregation, rootType, MongoCountResult::class.java))
            .thenReturn(countResults)

        // Act
        val result = executor.executePaged(
            aggregation,
            countAggregation,
            pageRequest,
            TestProjection::class.java
        )

        // Assert
        val page = result.page
        assertThat(page.content).containsExactlyElementsOf(expectedResults)
        assertThat(page.totalElements).isEqualTo(10)
        verify(mongoTemplate).aggregate(aggregation, rootType, TestProjection::class.java)
        verify(mongoTemplate).aggregate(countAggregation, rootType, MongoCountResult::class.java)
    }

    @ParameterizedTest(name = "page {0}, {1} items -> total {2}, no count")
    @CsvSource("0, 0, 0", "0, 14, 14", "2, 14, 64")
    fun `executePaged derives totals for partial pages without running count`(
        pageIndex: Int,
        size: Int,
        expectedTotal: Long,
    ) {
        // Arrange
        val data = mock<Aggregation>()
        val count = mock<Aggregation>()
        val items = List(size) { TestProjection("item$it") }
        val mapped = mock<AggregationResults<TestProjection>> { on { mappedResults }.thenReturn(items) }
        whenever(mongoTemplate.aggregate(data, rootType, TestProjection::class.java)).thenReturn(mapped)

        // Act
        val result = executor.executePaged(data, count, PageRequest.of(pageIndex, 25), TestProjection::class.java)

        // Assert
        assertThat(result.page.content).containsExactlyElementsOf(items)
        assertThat(result.page.totalElements).isEqualTo(expectedTotal)
        verify(mongoTemplate, never()).aggregate(count, rootType, MongoCountResult::class.java)
    }

    @Test
    fun `executePaged still counts an empty page beyond the last page`() {
        val data = mock<Aggregation>()
        val count = mock<Aggregation>()
        val mapped = mock<AggregationResults<TestProjection>> { on { mappedResults }.thenReturn(emptyList()) }
        val counted = mock<AggregationResults<MongoCountResult>> { on { uniqueMappedResult }.thenReturn(MongoCountResult(64)) }
        whenever(mongoTemplate.aggregate(data, rootType, TestProjection::class.java)).thenReturn(mapped)
        whenever(mongoTemplate.aggregate(count, rootType, MongoCountResult::class.java)).thenReturn(counted)

        val result = executor.executePaged(data, count, PageRequest.of(3, 25), TestProjection::class.java)

        assertThat(result.page.content).isEmpty()
        assertThat(result.page.totalElements).isEqualTo(64)
        verify(mongoTemplate).aggregate(count, rootType, MongoCountResult::class.java)
    }

    @Test
    fun `executePaged handles an empty count result when count is required`() {
        val data = mock<Aggregation>()
        val count = mock<Aggregation>()
        val mapped = mock<AggregationResults<TestProjection>> { on { mappedResults }.thenReturn(emptyList()) }
        val counted = mock<AggregationResults<MongoCountResult>> { on { uniqueMappedResult }.thenReturn(null) }
        whenever(mongoTemplate.aggregate(data, rootType, TestProjection::class.java)).thenReturn(mapped)
        whenever(mongoTemplate.aggregate(count, rootType, MongoCountResult::class.java)).thenReturn(counted)

        val result = executor.executePaged(data, count, PageRequest.of(1, 25), TestProjection::class.java)

        assertThat(result.page.totalElements).isZero()
        verify(mongoTemplate).aggregate(count, rootType, MongoCountResult::class.java)
    }

    // --------------------------------------------------------------------------------------------
    // findAll
    // --------------------------------------------------------------------------------------------

    @Test
    fun `findAll should delegate to MongoTemplate#findAll`() {
        // Arrange
        val expected = listOf(TestRoot("1", "A"), TestRoot("2", "B"))
        whenever(mongoTemplate.findAll(eq(TestRoot::class.java))).thenReturn(expected)

        // Act
        val result = executor.findAll(TestRoot::class.java)

        // Assert
        assertThat(result).isEqualTo(expected)
        verify(mongoTemplate).findAll(eq(TestRoot::class.java))
    }

    // --------------------------------------------------------------------------------------------
    // findPaged
    // --------------------------------------------------------------------------------------------

    @Test
    fun `findPaged should query the collection with paging and return results`() {
        // Arrange
        val pageRequest = PageRequest.of(0, 2)
        val collectionName = "test_collection"
        val expected = listOf(TestRoot("1", "foo"), TestRoot("2", "bar"))

        whenever(mongoTemplate.getCollectionName(rootType)).thenReturn(collectionName)
        whenever(
            mongoTemplate.find(
                any<Query>(),
                eq(TestRoot::class.java),
                eq(collectionName)
            )
        ).thenReturn(expected)
        whenever(
            mongoTemplate.count(any<Query>(), eq(collectionName))
        ).thenReturn(expected.size.toLong())

        // Act
        val result = executor.findPaged(pageRequest, TestRoot::class.java)

        // Assert
        val page = result.page
        assertThat(page.content).containsExactlyElementsOf(expected)
        assertThat(page.totalElements).isEqualTo(2)
        verify(mongoTemplate).find(any<Query>(), eq(TestRoot::class.java), eq(collectionName))
        verify(mongoTemplate).count(any<Query>(), eq(collectionName))
    }

    @Test
    fun `findPaged should return empty page`() {
        // Arrange
        val pageRequest = PageRequest.of(1, 5)
        val collectionName = "empty_collection"

        whenever(mongoTemplate.getCollectionName(rootType)).thenReturn(collectionName)
        whenever(
            mongoTemplate.find(any<Query>(), eq(TestRoot::class.java), eq(collectionName))
        ).thenReturn(emptyList())
        whenever(
            mongoTemplate.count(any<Query>(), eq(collectionName))
        ).thenReturn(0)

        // Act
        val result = executor.findPaged(pageRequest, TestRoot::class.java)

        // Assert
        assertThat(result.page.content).isEmpty()
        assertThat(result.page.totalElements).isZero()
    }
}
