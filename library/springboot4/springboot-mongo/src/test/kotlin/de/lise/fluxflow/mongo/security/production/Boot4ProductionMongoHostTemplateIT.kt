package de.lise.fluxflow.mongo.security.production

import de.lise.fluxflow.mongo.Boot4MongoIntegrationTest
import de.lise.fluxflow.mongo.Boot4MongoIntegrationTestConfig
import de.lise.fluxflow.mongo.MongoConfiguration
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@Boot4MongoIntegrationTest
@SpringBootTest(
    properties = ["fluxflow.mongo.enabled=true", "spring.data.mongodb.auto-index-creation=true"],
    classes = [Boot4MongoIntegrationTestConfig::class, MongoConfiguration::class,
        ProductionMongoSecurityConfiguration::class, ProductionMongoHostTemplateConfiguration::class,
        Boot4HostCommandConfiguration::class],
)
class Boot4ProductionMongoHostTemplateIT : AbstractProductionMongoHostTemplateIT()

@TestConfiguration
open class Boot4HostCommandConfiguration {
    @Bean open fun hostCommandCustomizer(commands: HostIndexCommands): MongoClientSettingsBuilderCustomizer =
        MongoClientSettingsBuilderCustomizer { it.addCommandListener(commands) }
}
