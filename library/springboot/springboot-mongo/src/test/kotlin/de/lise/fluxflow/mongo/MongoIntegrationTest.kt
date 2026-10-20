package de.lise.fluxflow.mongo

import de.lise.fluxflow.mongo.docker.DockerAvailableCondition
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.containers.MongoDBContainer

@Import(
    IntegrationTestConfig::class,
)
@ConditionalOnBean(MongoDBContainer::class)
@SpringBootTest(
    properties = ["fluxflow.mongo.enabled=true"],
    classes = [
        IntegrationTestConfig::class,
        MongoConfiguration::class
    ]
)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@ExtendWith(DockerAvailableCondition::class)
@Retention(AnnotationRetention.RUNTIME)
annotation class MongoIntegrationTest