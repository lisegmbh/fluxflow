package de.lise.fluxflow.mongo

import de.lise.fluxflow.mongo.docker.ConditionalOnDocker
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

@ConditionalOnDocker
@EnableAutoConfiguration
@SpringBootConfiguration
@TestConfiguration
open class IntegrationTestConfig {
    @Bean
    @ServiceConnection
    open fun mongoDbContainer(): MongoDBContainer? {
        return if(DockerClientFactory.instance().isDockerAvailable) {
            return MongoDBContainer(DockerImageName.parse("mongo:5.0"))
        } else null
    }
}

