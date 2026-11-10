package de.lise.fluxflow.mongo.workflow

import de.fluxflow.flowquery.query.FlowQuery
import de.fluxflow.flowquery.query.QueryConstraint
import de.lise.fluxflow.mongo.FluxFlowMongoQueryPipelineProvider
import de.lise.fluxflow.mongo.flowquery.repository.MongoFlowQueryRepository
import de.lise.fluxflow.mongo.flowquery.repository.MongoExecutor
import de.lise.fluxflow.mongo.flowquery.repository.MongoQueryTranslator
import java.util.function.Supplier
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.config.BeanDefinitionCustomizer
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.SimpleThreadScope
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.springframework.core.Ordered
import org.springframework.data.mongodb.core.aggregation.Aggregation

class WorkflowMongoQueryPipelineConfigurationTest {
    private object Restriction : QueryConstraint
    @Test
    fun `workflow configuration resolves provider beans in Spring order`() {
        GenericApplicationContext().use { context ->
            context.registerBean("later", FluxFlowMongoQueryPipelineProvider::class.java, Supplier { Provider(20) })
            context.registerBean("earlier", FluxFlowMongoQueryPipelineProvider::class.java, Supplier { Provider(10) })
            context.refresh()
            val executor = mock<MongoExecutor<WorkflowDocument>>()
            val repo = WorkflowMongoConfiguration().workflowFlowQueryRepository(
                mock<MongoQueryTranslator>(), executor, context.getBeanProvider(FluxFlowMongoQueryPipelineProvider::class.java))
            repo.withConstraint(Restriction).find(FlowQuery.of<WorkflowDocument>())
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

    @Test
    fun `request scoped providers are not constructed during startup or ordinary queries`() {
        GenericApplicationContext().use { context ->
            val requestActive = AtomicBoolean(false)
            val creations = AtomicInteger()
            context.beanFactory.registerScope("request", SimpleThreadScope())
            context.registerBean("requestProvider", FluxFlowMongoQueryPipelineProvider::class.java, Supplier {
                check(requestActive.get()) { "No request is active" }
                creations.incrementAndGet()
                Provider(10)
            }, BeanDefinitionCustomizer { it.scope = "request" })
            val executor = mock<MongoExecutor<WorkflowDocument>>()
            context.registerBean("workflowQueryRepository", MongoFlowQueryRepository::class.java, Supplier {
                WorkflowMongoConfiguration().workflowFlowQueryRepository(mock<MongoQueryTranslator>(), executor,
                    context.getBeanProvider(FluxFlowMongoQueryPipelineProvider::class.java))
            })
            context.refresh()
            @Suppress("UNCHECKED_CAST")
            val repo = context.getBean("workflowQueryRepository") as MongoFlowQueryRepository<WorkflowDocument>
            repo.find(FlowQuery.of<WorkflowDocument>())
            assertThat(creations.get()).isZero()
            requestActive.set(true)
            repo.withConstraint(Restriction).find(FlowQuery.of<WorkflowDocument>())
            assertThat(creations.get()).isEqualTo(1)
        }
    }

    @Test
    fun `a lazy provider can depend on the workflow query repository`() {
        GenericApplicationContext().use { context ->
            val creations = AtomicInteger()
            val executor = mock<MongoExecutor<WorkflowDocument>>()
            context.registerBean("workflowQueryRepository", MongoFlowQueryRepository::class.java, Supplier {
                WorkflowMongoConfiguration().workflowFlowQueryRepository(mock<MongoQueryTranslator>(), executor,
                    context.getBeanProvider(FluxFlowMongoQueryPipelineProvider::class.java))
            })
            context.registerBean("dependentProvider", FluxFlowMongoQueryPipelineProvider::class.java, Supplier {
                context.getBean("workflowQueryRepository")
                creations.incrementAndGet()
                Provider(10)
            }, BeanDefinitionCustomizer { it.isLazyInit = true })
            context.refresh()
            assertThat(creations.get()).isZero()
            @Suppress("UNCHECKED_CAST")
            val repo = context.getBean("workflowQueryRepository") as MongoFlowQueryRepository<WorkflowDocument>
            repo.withConstraint(Restriction).find(FlowQuery.of<WorkflowDocument>())
            assertThat(creations.get()).isEqualTo(1)
        }
    }

    private class Provider(private val sequence: Int) : FluxFlowMongoQueryPipelineProvider, Ordered {
        override fun getOrder() = sequence
        override fun stages(rootType: Class<*>, constraint: QueryConstraint) = listOf(
            Aggregation.stage(Document("\$match", Document("order", sequence)))
        ).also { assertThat(rootType).isEqualTo(WorkflowDocument::class.java) }
    }
}
