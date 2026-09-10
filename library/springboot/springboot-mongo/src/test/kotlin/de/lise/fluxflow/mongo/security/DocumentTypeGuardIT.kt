package de.lise.fluxflow.mongo.security

import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.MongoIntegrationTest
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener
import org.springframework.data.mongodb.core.mapping.event.AfterLoadEvent

/**
 * Proves the load-bearing assumption behind DocumentTypeGuard against a real MongoDB and the real
 * WorkflowMongoPersistence read path: that a listener's onAfterLoad exception actually reaches the
 * caller of a normal repository read, not just a hand-built ApplicationContext
 * (see EventPropagationSpikeTest for the framework-level proof).
 */
@MongoIntegrationTest
class DocumentTypeGuardIT {
    @Autowired
    lateinit var workflowPersistence: WorkflowPersistence

    @Autowired
    lateinit var applicationContext: ConfigurableApplicationContext

    @Test
    fun `an exception thrown from onAfterLoad propagates out of a real workflow read`() {
        // Arrange
        val saved = workflowPersistence.create(TestModel("a"), null)
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

    data class TestModel(val value: String)
}
