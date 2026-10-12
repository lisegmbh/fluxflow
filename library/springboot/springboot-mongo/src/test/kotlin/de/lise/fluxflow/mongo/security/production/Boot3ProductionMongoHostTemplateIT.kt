package de.lise.fluxflow.mongo.security.production

import de.lise.fluxflow.mongo.IntegrationTestConfig
import de.lise.fluxflow.mongo.MongoConfiguration
import de.lise.fluxflow.mongo.MongoIntegrationTest
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@MongoIntegrationTest
@SpringBootTest(
    properties = ["fluxflow.mongo.enabled=true", "spring.data.mongodb.auto-index-creation=true"],
    classes = [IntegrationTestConfig::class, MongoConfiguration::class,
        ProductionMongoSecurityConfiguration::class, ProductionMongoHostTemplateConfiguration::class,
        Boot3HostCommandConfiguration::class],
)
class Boot3ProductionMongoHostTemplateIT : AbstractProductionMongoHostTemplateIT()

@TestConfiguration
open class Boot3HostCommandConfiguration {
    @Bean open fun hostCommandCustomizer(commands: HostIndexCommands): MongoClientSettingsBuilderCustomizer =
        MongoClientSettingsBuilderCustomizer { it.addCommandListener(commands) }
}
