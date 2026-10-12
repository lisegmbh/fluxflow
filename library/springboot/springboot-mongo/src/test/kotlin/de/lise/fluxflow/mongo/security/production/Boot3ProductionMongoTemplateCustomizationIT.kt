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
    properties = ["fluxflow.mongo.enabled=true"],
    classes = [IntegrationTestConfig::class, MongoConfiguration::class,
        ProductionMongoSecurityConfiguration::class, ProductionMongoTemplateCustomizationConfiguration::class,
        Boot3CustomizedWriteCommandConfiguration::class],
)
class Boot3ProductionMongoTemplateCustomizationIT : AbstractProductionMongoTemplateCustomizationIT()

@TestConfiguration
open class Boot3CustomizedWriteCommandConfiguration {
    @Bean open fun customizedWriteCommandListener(commands: CustomizedWriteCommands): MongoClientSettingsBuilderCustomizer =
        MongoClientSettingsBuilderCustomizer { it.addCommandListener(commands) }
}
