package de.lise.fluxflow.mongo.workflow

import de.fluxflow.flowquery.query.FlowQuery
import de.lise.fluxflow.mongo.FluxFlowMongoQueryPipelineProvider
import de.lise.fluxflow.mongo.flowquery.repository.MongoExecutor
import de.lise.fluxflow.mongo.flowquery.repository.MongoQueryTranslator
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.Ordered
import org.springframework.data.mongodb.core.aggregation.Aggregation
import java.util.function.Supplier

class WorkflowMongoQueryPipelineConfigurationTest {
    @Test
    fun `workflow configuration resolves provider beans in Spring order`() {
        GenericApplicationContext().use { context ->
            context.registerBean("later", FluxFlowMongoQueryPipelineProvider::class.java, Supplier { Provider(20) })
            context.registerBean("earlier", FluxFlowMongoQueryPipelineProvider::class.java, Supplier { Provider(10) })
            context.refresh()
            val executor = mock<MongoExecutor<WorkflowDocument>>()
            val repo = WorkflowMongoConfiguration().workflowFlowQueryRepository(
                mock<MongoQueryTranslator>(), executor, context.getBeanProvider(FluxFlowMongoQueryPipelineProvider::class.java))
            repo.find(FlowQuery.of<WorkflowDocument>())
            val pipeline = argumentCaptor<Aggregation>()
            verify(executor).executeUnpaged(pipeline.capture(), eq(WorkflowDocument::class.java))
            assertThat(pipeline.firstValue.toPipeline(Aggregation.DEFAULT_CONTEXT)).containsExactly(
                Document("\$match", Document("order", 10)), Document("\$match", Document("order", 20)))
        }
    }

    @Test
    fun `existing direct configuration callers retain the ordinary path`() {
        val executor = mock<MongoExecutor<WorkflowDocument>>()
        val repo = WorkflowMongoConfiguration().workflowFlowQueryRepository(mock<MongoQueryTranslator>(), executor)
        repo.find(FlowQuery.of<WorkflowDocument>())
        verify(executor).findAll(WorkflowDocument::class.java)
    }

    private class Provider(private val sequence: Int) : FluxFlowMongoQueryPipelineProvider, Ordered {
        override fun getOrder() = sequence
        override fun stages(rootType: Class<*>) = listOf(
            Aggregation.stage(Document("\$match", Document("order", sequence)))
        ).also { assertThat(rootType).isEqualTo(WorkflowDocument::class.java) }
    }
}
