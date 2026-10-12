package de.lise.fluxflow.mongo.security.production

import com.mongodb.event.CommandListener
import com.mongodb.event.CommandStartedEvent
import de.lise.fluxflow.hostfixtures.IndexedHostBeforeAccess
import de.lise.fluxflow.hostfixtures.IndexedHostAfterAccess
import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.mongo.FluxFlowMongoConverterCustomizer
import de.lise.fluxflow.mongo.FluxFlowMongoTemplateCustomizer
import de.lise.fluxflow.mongo.FluxFlowMongoTypeMapperFactory
import de.lise.fluxflow.reflection.types.TypeRegistry
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.data.mapping.context.MappingContextEvent
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import org.springframework.data.projection.ProjectionFactory
import java.util.concurrent.CopyOnWriteArrayList

abstract class AbstractProductionMongoHostTemplateIT {
    @Autowired private lateinit var context: ConfigurableApplicationContext
    @Autowired private lateinit var host: MongoTemplate
    @Autowired private lateinit var registry: TypeRegistry
    @Autowired private lateinit var mapperFactory: FluxFlowMongoTypeMapperFactory
    @Autowired private lateinit var commands: HostIndexCommands
    @Autowired private lateinit var productionAccess: FluxFlowMongoAccess

    @Test
    fun `D11 internal Mongo access preserves host mapping events and creates each host index once`() {
        val mapping = host.converter.mappingContext as MongoMappingContext
        assertThat(mapping.isAutoIndexCreation).isTrue()
        assertThat(mapping.hasPersistentEntityFor(IndexedHostBeforeAccess::class.java)).isFalse()
        assertThat(mapping.hasPersistentEntityFor(IndexedHostAfterAccess::class.java)).isFalse()
        val observed = CopyOnWriteArrayList<Class<*>>()
        val listener = ApplicationListener<ApplicationEvent> { event ->
            if (event is MappingContextEvent<*, *> && event.source === mapping) {
                observed += event.persistentEntity.type
            }
        }
        context.addApplicationListener(listener)
        try {
            commands.collections.clear()
            host.save(IndexedHostBeforeAccess("before"))
            assertUniqueIndex("host_index_before_access")
            assertThat(observed.count { it == IndexedHostBeforeAccess::class.java }).isEqualTo(1)
            assertThat(commands.collections.count { it == "host_index_before_access" }).isEqualTo(1)

            commands.collections.clear()
            repeat(2) { newAccess() }
            assertThat(commands.collections)
                .describedAs("Constructing another internal access must not recreate host indexes")
                .isEmpty()
            host.save(IndexedHostAfterAccess("after"))
            assertUniqueIndex("host_index_after_access")
            assertThat(observed.count { it == IndexedHostAfterAccess::class.java }).isEqualTo(1)
            assertThat(commands.collections.count { it == "host_index_after_access" }).isEqualTo(1)
            assertThat(host.findAll(IndexedHostBeforeAccess::class.java)).hasSize(1)
            assertThat(host.findAll(IndexedHostAfterAccess::class.java)).hasSize(1)
            assertThat(productionAccess.template).isNotSameAs(host)
        } finally {
            context.removeApplicationListener(listener)
        }
    }

    private fun assertUniqueIndex(collection: String) {
        val indexes = host.getCollection(collection).listIndexes().toList()
        val unique = indexes.filter { it["key"] == Document("hostKey", 1) }
        assertThat(unique).hasSize(1)
        assertThat(unique.single().getBoolean("unique")).isTrue()
    }

    private fun newAccess(): FluxFlowMongoAccess = FluxFlowMongoAccess(
        host, registry, mapperFactory, context, context.beanFactory, context.environment,
        context.getBeanProvider(FluxFlowMongoConverterCustomizer::class.java),
        context.getBeanProvider(FluxFlowMongoTemplateCustomizer::class.java),
        context.getBeanProvider(ProjectionFactory::class.java),
    )
}

class HostIndexCommands : CommandListener {
    val collections = CopyOnWriteArrayList<String>()
    override fun commandStarted(event: CommandStartedEvent) {
        if (event.commandName == "createIndexes") {
            collections += event.command.getString("createIndexes").value
        }
    }
}

@TestConfiguration
open class ProductionMongoHostTemplateConfiguration {
    @Bean open fun hostIndexCommands(): HostIndexCommands = HostIndexCommands()
}
