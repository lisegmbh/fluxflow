package de.lise.fluxflow.mongo.security.baseline

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.set
import de.lise.fluxflow.mongo.flowquery.expression.compilation.MongoCompiler
import de.lise.fluxflow.mongo.flowquery.expression.compilation.SubclassProviderImpl
import de.lise.fluxflow.mongo.flowquery.repository.MongoQueryTranslator
import de.lise.fluxflow.mongo.workflow.WorkflowMongoConfiguration
import de.lise.fluxflow.mongo.workflow.WorkflowRepository
import de.lise.fluxflow.mongo.workflow.query.QueryableWorkflowRepositoryImpl
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import org.assertj.core.api.Assertions.assertThat
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.util.UUID

/** Real Mongo mapping and Fluxflow persistence, with fixture classes isolated per read scenario. */
internal class MongoSecurityHarness : AutoCloseable {
    val loader = WitnessClassLoader()
    private val container = SecurityMongoContainer()
    private lateinit var client: MongoClient
    val template: MongoTemplate
    val workflows: WorkflowPersistence

    init {
        container.start()
        try {
            client = MongoClients.create("mongodb://${container.host}:${container.getMappedPort(27017)}")
            template = MongoTemplate(client, "security_${UUID.randomUUID().toString().replace("-", "")}")
            val converter = template.converter as MappingMongoConverter
            converter.setTypeMapper(DefaultMongoTypeMapper("_class", converter.mappingContext).apply {
                setBeanClassLoader(loader)
            })
            val repository = MongoRepositoryFactory(template).getRepository(
                WorkflowRepository::class.java,
                RepositoryFragments.just(QueryableWorkflowRepositoryImpl(template))
            )
            val configuration = WorkflowMongoConfiguration()
            val translator = MongoQueryTranslator(MongoCompiler(SubclassProviderImpl(emptySet())))
            workflows = configuration.workflowPersistence(
                repository,
                configuration.workflowFlowQueryRepository(
                    translator,
                    configuration.workflowMongoExecutor(template)
                ),
                configuration.workflowDocumentMapper()
            )
        } catch (failure: Throwable) {
            if (::client.isInitialized) client.close()
            container.stop()
            throw failure
        }
    }

    fun tamper(type: Class<*>, id: String, path: String, value: String) {
        val collection = template.getCollection(template.getCollectionName(type))
        val update = collection.updateOne(eq("_id", id), set(path, value))
        assertThat(update.matchedCount).describedAs("The legitimate document exists").isEqualTo(1)
        assertThat(update.modifiedCount).describedAs("The raw document was changed").isEqualTo(1)
        val raw = requireNotNull(collection.find(eq("_id", id)).first())
        val storedValue = path.split('.').fold(raw as Any?) { current, field ->
            (current as Map<*, *>)[field]
        }
        assertThat(storedValue).isEqualTo(value)
    }

    override fun close() {
        try {
            client.close()
        } finally {
            container.stop()
        }
    }

    private class SecurityMongoContainer : GenericContainer<SecurityMongoContainer>(
        DockerImageName.parse("mongo:8.0.12")
    ) {
        init {
            withExposedPorts(27017)
            waitingFor(Wait.forListeningPort())
        }
    }
}

internal const val WITNESS_NAME =
    "de.lise.fluxflow.mongo.security.baseline.fixture.ActivationWitness"
