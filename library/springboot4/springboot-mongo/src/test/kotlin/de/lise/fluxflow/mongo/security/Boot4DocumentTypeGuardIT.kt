package de.lise.fluxflow.mongo.security

import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.Boot4MongoIntegrationTest
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
 * Mirrors DocumentTypeGuardIT (springboot:springboot-mongo) against spring-data-mongodb 5.x
 * (Boot 4 line). Only the main sourceSet is shared between the two Boot lines - this spike has to
 * be proven separately here, since the event/converter machinery underneath is a different major
 * version on this line.
 */
@Boot4MongoIntegrationTest
@Import(Boot4DocumentTypeGuardIT.ThrowingListenerConfig::class)
class Boot4DocumentTypeGuardIT {
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
