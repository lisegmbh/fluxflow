package de.lise.fluxflow.mongo

import de.lise.fluxflow.mongo.docker.ConditionalOnDocker
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.DockerClientFactory
import org.testcontainers.mongodb.MongoDBContainer

@ConditionalOnDocker
@EnableAutoConfiguration
@SpringBootConfiguration
@TestConfiguration
open class Boot4MongoIntegrationTestConfig {
    @Bean
    @ServiceConnection
    open fun mongoDbContainer(): MongoDBContainer? {
        return if (DockerClientFactory.instance().isDockerAvailable) {
            MongoDBContainer("mongo").withReplicaSet()
        } else {
            null
        }
    }
}
