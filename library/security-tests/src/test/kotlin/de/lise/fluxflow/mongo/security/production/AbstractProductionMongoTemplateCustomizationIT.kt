package de.lise.fluxflow.mongo.security.production

import com.mongodb.WriteConcern
import com.mongodb.event.CommandListener
import com.mongodb.event.CommandStartedEvent
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.FluxFlowMongoTemplateCustomizer
import de.lise.fluxflow.mongo.security.fixtures.HostOnlyWorkflowModel
import de.lise.fluxflow.mongo.security.fixtures.SecurityHostDocument
import de.lise.fluxflow.mongo.security.fixtures.securityTestModel
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import org.assertj.core.api.Assertions.assertThat
import org.bson.BsonDocument
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.MongoTransactionManager
import org.springframework.data.mongodb.SessionSynchronization
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.WriteResultChecking
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

abstract class AbstractProductionMongoTemplateCustomizationIT {
    @Autowired private lateinit var host: MongoTemplate
    @Autowired private lateinit var factory: MongoDatabaseFactory
    @Autowired private lateinit var workflows: WorkflowPersistence
    @Autowired private lateinit var writes: CustomizedWriteCommands

    @BeforeEach fun clear() {
        listOf(WorkflowDocument::class.java, SecurityHostDocument::class.java).forEach {
            host.getCollection(host.getCollectionName(it)).deleteMany(org.bson.Document())
        }
        writes.insertConcerns.clear()
        writes.resolvedDefaults.clear()
    }

    @Test
    fun `template customizer controls effective workflow write concern and resolver`() {
        val identifier = WorkflowIdentifier(UUID.randomUUID().toString())
        workflows.create(securityTestModel(), identifier)

        val collection = host.getCollectionName(WorkflowDocument::class.java)
        val concerns = writes.insertConcerns.filter { it.first == collection }.map { it.second }
        assertThat(concerns).hasSize(1)
        assertThat(concerns.single().getString("w").value).isEqualTo("majority")
        assertThat(concerns.single().getNumber("wtimeout").longValue()).isEqualTo(5_000L)
        assertThat(writes.resolvedDefaults.filter { it.first == WorkflowDocument::class.java }
            .map { it.second }).containsExactly(WriteConcern.MAJORITY)
        assertThat(workflows.find(identifier)?.model).isEqualTo(securityTestModel())
    }

    @Test
    fun `template customizer NEVER session synchronization keeps workflow write outside host transaction`() {
        val identifier = WorkflowIdentifier(UUID.randomUUID().toString())
        val hostDocument = SecurityHostDocument(UUID.randomUUID().toString(), HostOnlyWorkflowModel("host transaction"))
        TransactionTemplate(MongoTransactionManager(factory)).executeWithoutResult { transaction ->
            host.save(hostDocument)
            workflows.create(securityTestModel(), identifier)
            transaction.setRollbackOnly()
        }

        assertThat(host.findById(hostDocument.id, SecurityHostDocument::class.java)).isNull()
        assertThat(workflows.find(identifier)?.model).isEqualTo(securityTestModel())
    }
}

class CustomizedWriteCommands : CommandListener {
    val insertConcerns = CopyOnWriteArrayList<Pair<String, BsonDocument>>()
    val resolvedDefaults = CopyOnWriteArrayList<Pair<Class<*>?, WriteConcern?>>()
    override fun commandStarted(event: CommandStartedEvent) {
        if (event.commandName == "insert") {
            insertConcerns += event.command.getString("insert").value to
                event.command.getDocument("writeConcern", BsonDocument()).clone()
        }
    }
}

@TestConfiguration
open class ProductionMongoTemplateCustomizationConfiguration {
    @Bean open fun customizedWriteCommands(): CustomizedWriteCommands = CustomizedWriteCommands()

    @Bean @Order(30)
    open fun explicitMongoWriteSettings(writes: CustomizedWriteCommands): FluxFlowMongoTemplateCustomizer =
        FluxFlowMongoTemplateCustomizer { template ->
            template.setWriteConcern(WriteConcern.MAJORITY)
            template.setWriteConcernResolver { action ->
                writes.resolvedDefaults += action.entityType to action.defaultWriteConcern
                WriteConcern.MAJORITY.withWTimeout(5_000, TimeUnit.MILLISECONDS)
            }
            template.setWriteResultChecking(WriteResultChecking.EXCEPTION)
            template.setSessionSynchronization(SessionSynchronization.NEVER)
        }
}
