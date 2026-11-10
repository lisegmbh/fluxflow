package de.lise.fluxflow.mongo.flowquery.repository

import de.fluxflow.flowquery.query.FlowQuery
import de.fluxflow.flowquery.query.QueryConstraint
import de.fluxflow.flowquery.query.QueryExecutionException
import de.lise.fluxflow.mongo.FluxFlowMongoQueryPipelineProvider
import de.lise.fluxflow.mongo.flowquery.expression.compilation.MongoCompiler
import de.lise.fluxflow.mongo.flowquery.expression.compilation.SubclassProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bson.Document
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.aggregation.Aggregation

class MongoQueryPipelineProviderTest {
    private object Restriction : QueryConstraint
    private val executor = mock<MongoExecutor<RootDocument>>()
    private val translator = MongoQueryTranslator(MongoCompiler(mock<SubclassProvider>()))
    private val filter = Aggregation.stage(Document("\$match", Document("readable", true)))

    @Test
    fun `provider prevents the unpaged empty-query shortcut from bypassing its stages`() {
        val provider = mock<FluxFlowMongoQueryPipelineProvider>()
        whenever(provider.stages(RootDocument::class.java, Restriction)).thenReturn(listOf(filter))
        repository(provider).find(FlowQuery.of<RootDocument>())
        assertThat(unpagedStages()).containsExactly(Document("\$match", Document("readable", true)))
        verify(provider, times(1)).stages(RootDocument::class.java, Restriction)
        verify(executor, never()).findAll(any<Class<Any>>())
    }

    @Test
    fun `page and count share one captured provider result before pagination`() {
        var calls = 0
        val provider = FluxFlowMongoQueryPipelineProvider { root, constraint ->
            assertThat(constraint).isSameAs(Restriction)
            assertThat(root).isEqualTo(RootDocument::class.java)
            calls++
            listOf(Aggregation.stage(Document("\$match", Document("scope", calls))))
        }
        whenever(executor.executePaged(any(), any(), any(), eq(RootDocument::class.java)))
            .thenReturn(PagedMongoResults(PageImpl(emptyList(), PageRequest.of(1, 2), 0)))
        repository(provider).find { paged(1, 2) }
        val page = argumentCaptor<Aggregation>()
        val count = argumentCaptor<Aggregation>()
        verify(executor).executePaged(page.capture(), count.capture(), eq(PageRequest.of(1, 2)), eq(RootDocument::class.java))
        val pageStages = pipeline(page.firstValue)
        val countStages = pipeline(count.firstValue)
        assertThat(calls).isEqualTo(1)
        assertThat(pageStages.first()).isEqualTo(Document("\$match", Document("scope", 1)))
        assertThat(countStages.first()).isEqualTo(pageStages.first())
        assertThat(pageStages.map { it.keys.first() }).containsExactly("\$match", "\$skip", "\$limit")
        assertThat(countStages.map { it.keys.first() }).containsExactly("\$match", "\$count")
        verify(executor, never()).findPaged(any(), any<Class<Any>>())
    }

    @Test
    fun `provider sees persisted root and runs before projection and explicit limit`() {
        val provider = mock<FluxFlowMongoQueryPipelineProvider>()
        whenever(provider.stages(RootDocument::class.java, Restriction)).thenReturn(listOf(filter))
        repository(provider).find(ProjectedDocument::class.java,
            FlowQuery.of<RootDocument>().project { get(RootDocument::projected) }.limit(1))
        val captured = argumentCaptor<Aggregation>()
        verify(executor).executeUnpaged(captured.capture(), eq(ProjectedDocument::class.java))
        assertThat(pipeline(captured.firstValue).map { it.keys.first() })
            .containsExactly("\$match", "\$project", "\$replaceRoot", "\$limit")
        verify(provider, times(1)).stages(RootDocument::class.java, Restriction)
    }

    @Test
    fun `first and single queries apply providers before page selection and result cap`() {
        val provider = mock<FluxFlowMongoQueryPipelineProvider>()
        whenever(provider.stages(RootDocument::class.java, Restriction)).thenReturn(listOf(filter))
        repository(provider).findFirstOrNull { paged(1, 2) }
        assertThat(unpagedStages().map { it.keys.first() })
            .containsExactly("\$match", "\$skip", "\$limit", "\$limit")
        assertThat(unpagedStages().last()).isEqualTo(Document("\$limit", 1L))
        verify(executor, never()).executePaged(any(), any(), any(), any<Class<Any>>())
        verify(provider, times(1)).stages(RootDocument::class.java, Restriction)
        clearInvocations(executor, provider)
        repository(provider).findSingleOrNull(FlowQuery.of<RootDocument>())
        assertThat(unpagedStages()).containsExactly(
            Document("\$match", Document("readable", true)), Document("\$limit", 2L))
        verify(provider, times(1)).stages(RootDocument::class.java, Restriction)
    }

    @Test
    fun `empty provider result preserves ordinary find paths`() {
        val repo = repository(FluxFlowMongoQueryPipelineProvider { _, _ -> emptyList() })
        repo.find(FlowQuery.of<RootDocument>())
        verify(executor).findAll(RootDocument::class.java)
        whenever(executor.findPaged(any(), eq(RootDocument::class.java)))
            .thenReturn(PagedMongoResults(PageImpl(emptyList(), PageRequest.of(0, 2), 0)))
        repo.find { paged(0, 2) }
        verify(executor).findPaged(PageRequest.of(0, 2), RootDocument::class.java)
        verify(executor, never()).executeUnpaged(any(), any<Class<Any>>())
        verify(executor, never()).executePaged(any(), any(), any(), any<Class<Any>>())
    }

    @Test
    fun `provider failure propagates before any query can return a partial result`() {
        val failure = IllegalStateException("scope unavailable")
        assertThatThrownBy {
            repository(FluxFlowMongoQueryPipelineProvider { _, _ -> throw failure }).find(FlowQuery.of<RootDocument>())
        }.isSameAs(failure)
        verifyNoInteractions(executor)
    }

    @Test
    fun `multiple providers retain their declared order`() {
        repository(
            FluxFlowMongoQueryPipelineProvider { _, _ -> listOf(Aggregation.stage(Document("\$match", Document("first", true)))) },
            FluxFlowMongoQueryPipelineProvider { _, _ -> listOf(filter) },
        ).find(FlowQuery.of<RootDocument>())
        assertThat(unpagedStages()).containsExactly(
            Document("\$match", Document("first", true)), Document("\$match", Document("readable", true)))
    }

    @Test
    fun `ordinary calls never consult constraint providers`() {
        val provider = mock<FluxFlowMongoQueryPipelineProvider>()
        MongoFlowQueryRepository(RootDocument::class.java, translator, executor, listOf(provider))
            .find(FlowQuery.of<RootDocument>())
        verify(executor).findAll(RootDocument::class.java)
        verifyNoInteractions(provider)
    }

    @Test
    fun `missing or unsupported providers fail before database execution`() {
        assertThatThrownBy { repository().find(FlowQuery.of<RootDocument>()) }
            .isInstanceOf(QueryExecutionException::class.java)
        assertThatThrownBy {
            repository(FluxFlowMongoQueryPipelineProvider { _, _ -> null }).find(FlowQuery.of<RootDocument>())
        }.isInstanceOf(QueryExecutionException::class.java)
        verifyNoInteractions(executor)
    }

    @Test
    fun `explicit repository views cannot replace an existing constraint`() {
        assertThatThrownBy { repository().withConstraint(Restriction) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    private fun repository(vararg providers: FluxFlowMongoQueryPipelineProvider) =
        MongoFlowQueryRepository(RootDocument::class.java, translator, executor, providers.toList()).withConstraint(Restriction)

    private fun unpagedStages(): List<Document> {
        val captured = argumentCaptor<Aggregation>()
        verify(executor).executeUnpaged(captured.capture(), eq(RootDocument::class.java))
        return pipeline(captured.firstValue)
    }

    private fun pipeline(aggregation: Aggregation) = aggregation.toPipeline(Aggregation.DEFAULT_CONTEXT)

    data class RootDocument(val id: String, val readable: Boolean, val projected: ProjectedDocument)
    data class ProjectedDocument(val id: String)
}
