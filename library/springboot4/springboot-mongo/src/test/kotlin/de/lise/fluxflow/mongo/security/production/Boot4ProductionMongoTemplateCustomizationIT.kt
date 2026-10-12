package de.lise.fluxflow.mongo.security.production

import de.lise.fluxflow.mongo.Boot4MongoIntegrationTestConfig
import de.lise.fluxflow.mongo.MongoConfiguration
import de.lise.fluxflow.mongo.Boot4MongoIntegrationTest
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@Boot4MongoIntegrationTest
@SpringBootTest(
    properties = ["fluxflow.mongo.enabled=true"],
    classes = [Boot4MongoIntegrationTestConfig::class, MongoConfiguration::class,
        ProductionMongoSecurityConfiguration::class, ProductionMongoTemplateCustomizationConfiguration::class,
        Boot4CustomizedWriteCommandConfiguration::class],
)
class Boot4ProductionMongoTemplateCustomizationIT : AbstractProductionMongoTemplateCustomizationIT()

@TestConfiguration
open class Boot4CustomizedWriteCommandConfiguration {
    @Bean open fun customizedWriteCommandListener(commands: CustomizedWriteCommands): MongoClientSettingsBuilderCustomizer =
        MongoClientSettingsBuilderCustomizer { it.addCommandListener(commands) }
}
