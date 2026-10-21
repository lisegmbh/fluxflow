package de.lise.fluxflow.mongo.flowquery.repository

import de.fluxflow.flowquery.query.sorting.Sort.Companion.asc
import de.lise.fluxflow.mongo.flowquery.expression.compilation.MongoCompiler
import de.lise.fluxflow.mongo.flowquery.expression.compilation.SubclassProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bson.Document
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.aggregation.Aggregation

/**
 * Unit tests for [MongoFlowQueryRepository] that do not require Docker.
 *
 * Integration tests in [MongoQueryRepositoryIT] are gated on a running Docker daemon
 * via [@MongoIntegrationTest][de.lise.fluxflow.mongo.MongoIntegrationTest].
 */
class MongoFlowQueryRepositoryTest {

    private lateinit var translator: MongoQueryTranslator
    private lateinit var executor: MongoExecutor<WorkflowDocument>
    private lateinit var repo: MongoFlowQueryRepository<WorkflowDocument>

    @BeforeEach
    fun setup() {
        val subclassProvider: SubclassProvider = mock()
        translator = MongoQueryTranslator(MongoCompiler(subclassProvider))
        executor = mock()
        repo = MongoFlowQueryRepository(WorkflowDocument::class.java, translator, executor)
    }

    @Test
    fun `paged query pipeline should sort with unique tiebreaker before skip and limit`() {
        // Arrange
        whenever(
            executor.executePaged(any(), any(), any(), eq(WorkflowDocument::class.java))
        ).thenReturn(
            PagedMongoResults(PageImpl(emptyList(), PageRequest.of(1, 5), 0))
        )

        // Act
        repo.find {
            sort {
                get(WorkflowDocument::model)
                    .get(WorkflowModel::metaInformationen)
                    .get(MetaInformationen::bearbeitet)
                    .get(ModificationInfo::actor)
                    .get(User::lastName)
                    .asc()
            }.paged(pageIndex = 1, pageSize = 5)
        }

        // Assert
        val pagedAggregation = argumentCaptor<Aggregation>()
        verify(executor).executePaged(
            pagedAggregation.capture(),
            any(),
            eq(PageRequest.of(1, 5)),
            eq(WorkflowDocument::class.java),
        )

        val stages = pagedAggregation.firstValue.toPipeline(Aggregation.DEFAULT_CONTEXT)
        val stageKeys = stages.map { it.keys.first() }

        assertThat(stageKeys).containsSubsequence("\$sort", "\$skip", "\$limit")

        val sortSpec = stages.single { it.containsKey("\$sort") }["\$sort"] as Document
        assertThat(sortSpec.keys).containsExactly(
            "model.metaInformationen.bearbeitet.actor.lastName",
            "_id",
        )
        assertThat(sortSpec.getInteger("_id")).isEqualTo(1)

        assertThat(stages.single { it.containsKey("\$skip") }["\$skip"]).isEqualTo(5L)
        assertThat(stages.single { it.containsKey("\$limit") }["\$limit"]).isEqualTo(5L)
    }

    @Test
    fun `paged findFirst should cap the result after selecting the page without counting`() {
        // Act
        repo.findFirstOrNull {
            sort { get(WorkflowDocument::id).asc() }.paged(pageIndex = 1, pageSize = 5)
        }

        // Assert
        val stages = capturedUnpagedStages()
        assertThat(stages.takeLast(3)).containsExactly(
            Document("\$skip", 5L),
            Document("\$limit", 5L),
            Document("\$limit", 1L),
        )
        verify(executor, never()).executePaged(any(), any(), any(), any<Class<Any>>())
    }

    @Test
    fun `unpaged findSingle should cap the result to two elements`() {
        // Act
        repo.findSingleOrNull {
            sort { get(WorkflowDocument::id).asc() }
        }

        // Assert
        val stages = capturedUnpagedStages()
        assertThat(stages.last()).isEqualTo(Document("\$limit", 2L))
        assertThat(stages).noneMatch { it.containsKey("\$skip") }
    }

    @Test
    fun `paged findFirst should not overflow the skip offset`() {
        // Act
        repo.findFirstOrNull {
            sort { get(WorkflowDocument::id).asc() }.paged(pageIndex = 2_000_000_000, pageSize = 2)
        }

        // Assert
        assertThat(capturedUnpagedStages().single { it.containsKey("\$skip") }["\$skip"]).isEqualTo(4_000_000_000L)
    }

    @Test
    fun `findFirst should reject an invalid page size before executing`() {
        assertThatThrownBy {
            repo.findFirstOrNull { paged(pageIndex = 0, pageSize = 0) }
        }.isInstanceOf(IllegalArgumentException::class.java)

        verifyNoInteractions(executor)
    }

    private fun capturedUnpagedStages(): List<Document> {
        val aggregation = argumentCaptor<Aggregation>()
        verify(executor).executeUnpaged(aggregation.capture(), eq(WorkflowDocument::class.java))
        return aggregation.firstValue.toPipeline(Aggregation.DEFAULT_CONTEXT)
    }

    data class WorkflowDocument(val id: String, val model: WorkflowModel)

    data class WorkflowModel(val metaInformationen: MetaInformationen)

    data class MetaInformationen(val bearbeitet: ModificationInfo)

    data class ModificationInfo(val actor: User)

    data class User(val lastName: String)
}
