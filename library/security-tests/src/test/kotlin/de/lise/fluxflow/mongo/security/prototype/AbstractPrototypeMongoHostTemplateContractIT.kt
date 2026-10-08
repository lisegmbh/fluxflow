package de.lise.fluxflow.mongo.security.prototype

import com.mongodb.ReadPreference
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.mapping.context.MappingContextEvent
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.mapping.MongoMappingContext

abstract class AbstractPrototypeMongoHostTemplateContractIT {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var hostTemplate: MongoTemplate

    @Autowired
    private lateinit var databaseFactory: MongoDatabaseFactory

    @Autowired
    private lateinit var typeMapperFactory: PrototypeMongoTypeMapperFactory

    @Test
    fun `D11 prototype access preserves the fixture host read preference`() {
        val access = prototypeAccess()

        assertThat(hostTemplate.hasReadPreference()).isTrue()
        assertThat(hostTemplate.readPreference).isEqualTo(ReadPreference.secondaryPreferred())
        assertThat(access.template.hasReadPreference()).isTrue()
        assertThat(access.template.readPreference).isEqualTo(hostTemplate.readPreference)
    }

    @Test
    fun `D11 prototype access leaves host mapping events listeners and auto indexes intact`() {
        val hostConverter = hostTemplate.converter as MappingMongoConverter
        val hostMappingContext = hostConverter.mappingContext as MongoMappingContext
        val observedTypes = mutableSetOf<Class<*>>()
        val listener = ApplicationListener<ApplicationEvent> { event ->
            (event as? MappingContextEvent<*, *>)
                ?.persistentEntity
                ?.type
                ?.let(observedTypes::add)
        }
        val context = applicationContext as ConfigurableApplicationContext

        assertThat(hostMappingContext.isAutoIndexCreation).isTrue()
        context.addApplicationListener(listener)
        try {
            hostMappingContext.getPersistentEntity(HostMappedBeforePrototype::class.java)
            assertThat(observedTypes).contains(HostMappedBeforePrototype::class.java)

            val access = prototypeAccess()
            access.template.collectionExists(WorkflowDocument::class.java)

            hostMappingContext.getPersistentEntity(HostMappedAfterPrototype::class.java)
            assertThat(observedTypes).contains(HostMappedAfterPrototype::class.java)
            assertThat(hostMappingContext.isAutoIndexCreation).isTrue()
        } finally {
            context.removeApplicationListener(listener)
        }
    }

    private fun prototypeAccess(): PrototypeFluxFlowMongoAccess = PrototypeFluxFlowMongoAccess(
        hostTemplate = hostTemplate,
        databaseFactory = databaseFactory,
        registry = prototypeRegistry(WitnessClassLoader(), *allowedPrototypeEntries()),
        typeMapperFactory = typeMapperFactory,
    )

    private class HostMappedBeforePrototype

    private class HostMappedAfterPrototype
}
