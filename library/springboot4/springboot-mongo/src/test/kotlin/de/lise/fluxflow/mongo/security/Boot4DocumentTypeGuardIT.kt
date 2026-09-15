package de.lise.fluxflow.mongo.security

import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.Boot4MongoIntegrationTest
import de.lise.fluxflow.mongo.e2e.Boot4MongoE2EWorkflowModel
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener
import org.springframework.data.mongodb.core.mapping.event.AfterLoadEvent

/**
 * Mirrors DocumentTypeGuardIT (springboot:springboot-mongo) against spring-data-mongodb 5.x
 * (Boot 4 line). Only the main sourceSet is shared between the two Boot lines - this spike has to
 * be proven separately here, since the event/converter machinery underneath is a different major
 * version on this line.
 */
@Boot4MongoIntegrationTest
class Boot4DocumentTypeGuardIT {
    @Autowired
    lateinit var workflowPersistence: WorkflowPersistence

    @Autowired
    lateinit var applicationContext: ConfigurableApplicationContext

    @Test
    fun `an exception thrown from onAfterLoad propagates out of a real workflow read`() {
        // Arrange
        val saved = workflowPersistence.create(Boot4MongoE2EWorkflowModel("a"), null)
        val listener = ThrowingDocumentListener()
        applicationContext.addApplicationListener(listener)

        // Act & Assert
        try {
            assertThatThrownBy {
                workflowPersistence.find(WorkflowIdentifier(saved.id))
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessage("boom")
        } finally {
            applicationContext.removeApplicationListener(listener)
        }
    }

    class ThrowingDocumentListener : AbstractMongoEventListener<Any>() {
        override fun onAfterLoad(event: AfterLoadEvent<Any>) {
            throw IllegalStateException("boom")
        }
    }
}
