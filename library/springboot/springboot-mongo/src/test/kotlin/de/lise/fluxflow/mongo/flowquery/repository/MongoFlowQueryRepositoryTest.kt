package de.lise.fluxflow.mongo.flowquery.repository

import de.fluxflow.flowquery.query.sorting.Sort.Companion.asc
import de.fluxflow.flowquery.repository.NonProjectingRepository.Companion.find
import de.lise.fluxflow.mongo.flowquery.expression.compilation.MongoCompiler
import de.lise.fluxflow.mongo.flowquery.expression.compilation.SubclassProvider
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
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

    data class WorkflowDocument(val id: String, val model: WorkflowModel)

    data class WorkflowModel(val metaInformationen: MetaInformationen)

    data class MetaInformationen(val bearbeitet: ModificationInfo)

    data class ModificationInfo(val actor: User)

    data class User(val lastName: String)
}
