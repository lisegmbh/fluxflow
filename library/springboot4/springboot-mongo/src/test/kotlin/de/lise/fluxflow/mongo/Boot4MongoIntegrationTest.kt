package de.lise.fluxflow.mongo

import de.lise.fluxflow.mongo.docker.DockerAvailableCondition
import de.lise.fluxflow.mongo.e2e.Boot4MongoE2ETestConfiguration
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.mongodb.MongoDBContainer

@Import(Boot4MongoIntegrationTestConfig::class)
@ConditionalOnBean(MongoDBContainer::class)
@SpringBootTest(
    properties = ["fluxflow.mongo.enabled=true"],
    classes = [
        Boot4MongoIntegrationTestConfig::class,
        Boot4MongoE2ETestConfiguration::class,
        MongoConfiguration::class,
    ],
)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@ExtendWith(DockerAvailableCondition::class)
@Retention(AnnotationRetention.RUNTIME)
annotation class Boot4MongoIntegrationTest
