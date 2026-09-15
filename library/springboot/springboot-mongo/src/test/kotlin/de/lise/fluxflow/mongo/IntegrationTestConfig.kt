package de.lise.fluxflow.mongo

import de.lise.fluxflow.mongo.docker.ConditionalOnDocker
import de.lise.fluxflow.mongo.flowquery.repository.MongoQueryRepositoryIT
import de.lise.fluxflow.mongo.security.DocumentTypeGuardIT
import de.lise.fluxflow.mongo.workflow.WorkflowMongoPersistenceIT
import de.lise.fluxflow.reflection.types.TypeManifestEntry
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.MongoDBContainer

@ConditionalOnDocker
@EnableAutoConfiguration
@SpringBootConfiguration
@TestConfiguration
open class IntegrationTestConfig {
    @Bean
    @ServiceConnection
    open fun mongoDbContainer(): MongoDBContainer? {
        return if (DockerClientFactory.instance().isDockerAvailable) {
            MongoDBContainer("mongo")
        } else {
            null
        }
    }

    @Bean
    open fun integrationTestTypeRegistry(): TypeRegistry = TypeRegistry.create(
        IntegrationTestConfig::class.java.classLoader,
        listOf(
            testType(TypeRole.MODEL, WorkflowMongoPersistenceIT.TestModel::class.java),
            testType(TypeRole.VALUE, WorkflowMongoPersistenceIT.NestedTestModel::class.java),
            testType(TypeRole.MODEL, DocumentTypeGuardIT.TestModel::class.java),
            testType(TypeRole.VALUE, MongoQueryRepositoryIT.NestedTestDocument::class.java),
        ),
    )

    private fun testType(role: TypeRole, type: Class<*>): TypeManifestEntry =
        TypeManifestEntry(role, type.name, type.name, "Boot 3 Mongo integration test fixture")
}
