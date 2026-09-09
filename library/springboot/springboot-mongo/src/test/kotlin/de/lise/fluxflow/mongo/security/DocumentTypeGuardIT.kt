package de.lise.fluxflow.mongo.security

import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.MongoIntegrationTest
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener
import org.springframework.data.mongodb.core.mapping.event.AfterLoadEvent

/**
 * Proves the load-bearing assumption behind DocumentTypeGuard against a real MongoDB and the real
 * WorkflowMongoPersistence read path: that a listener's onAfterLoad exception actually reaches the
 * caller of a normal repository read, not just a hand-built ApplicationContext
 * (see EventPropagationSpikeTest for the framework-level proof).
 */
@MongoIntegrationTest
@Import(DocumentTypeGuardIT.ThrowingListenerConfig::class)
class DocumentTypeGuardIT {
    @Autowired
    lateinit var workflowPersistence: WorkflowPersistence

    @Test
    fun `an exception thrown from onAfterLoad propagates out of a real workflow read`() {
        // Arrange
        val saved = workflowPersistence.create(TestModel("a"), null)

        // Act & Assert
        assertThatThrownBy {
            workflowPersistence.find(WorkflowIdentifier(saved.id))
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessage("boom")
    }

    @TestConfiguration
    class ThrowingListenerConfig {
        @Bean
        fun throwingWorkflowDocumentListener(): ThrowingWorkflowDocumentListener {
            return ThrowingWorkflowDocumentListener()
        }
    }

    class ThrowingWorkflowDocumentListener : AbstractMongoEventListener<WorkflowDocument>() {
        override fun onAfterLoad(event: AfterLoadEvent<WorkflowDocument>) {
            throw IllegalStateException("boom")
        }
    }

    data class TestModel(val value: String)
}
