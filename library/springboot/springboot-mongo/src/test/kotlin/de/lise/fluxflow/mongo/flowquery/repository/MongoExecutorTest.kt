package de.lise.fluxflow.mongo.flowquery.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
    fun `executeUnpaged should delegate to MongoTemplateaggregate and return mapped results`() {
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

    @Test
    fun `executePaged should default total count to results size when countAggregation returns null`() {
        // Arrange
        val aggregation = mock<Aggregation>()
        val countAggregation = mock<Aggregation>()
        val pageRequest = PageRequest.of(1, 5)
        val expectedResults = listOf(TestProjection("foo"))

        val aggregationResults: AggregationResults<TestProjection> = mock {
            on { mappedResults }.thenReturn(expectedResults)
        }
        val countResults: AggregationResults<MongoCountResult> = mock {
            on { uniqueMappedResult }.thenReturn(null)
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
        // Spring Data counts based on offset + current page size when count supplier returns null
        assertThat(result.page.content).hasSize(1)
        assertThat(result.page.totalElements).isEqualTo(6)
    }

    // --------------------------------------------------------------------------------------------
    // findAll
    // --------------------------------------------------------------------------------------------

    @Test
    fun `findAll should delegate to MongoTemplatefindAll`() {
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
