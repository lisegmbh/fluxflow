package de.lise.fluxflow.mongo.security

import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bson.Document
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.config.BeanDefinitionCustomizer
import org.springframework.context.support.GenericApplicationContext
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener
import org.springframework.data.mongodb.core.mapping.event.AfterLoadEvent

/**
 * Proves the load-bearing assumption behind the whole DocumentTypeGuard approach without needing
 * MongoDB/Testcontainers: that an exception thrown from AbstractMongoEventListener.onAfterLoad
 * propagates synchronously out of ApplicationEventPublisher.publishEvent, exactly as
 * EntityLifecycleEventDelegate.publishEvent (which MongoTemplate delegates to) invokes it.
 */
class EventPropagationSpikeTest {

    private class ThrowingListener : AbstractMongoEventListener<WorkflowDocument>() {
        override fun onAfterLoad(event: AfterLoadEvent<WorkflowDocument>) {
            throw IllegalStateException("boom")
        }
    }

    @Test
    fun `an exception thrown from onAfterLoad propagates synchronously out of publishEvent`() {
        // Arrange
        val context = GenericApplicationContext()
        // Explicit empty BeanDefinitionCustomizer vararg disambiguates Kotlin's overload
        // resolution between registerBean's two vararg overloads (constructorArgs vs.
        // customizers), which are otherwise ambiguous when called with zero arguments.
        context.registerBean(ThrowingListener::class.java, *arrayOf<BeanDefinitionCustomizer>())
        context.refresh()

        // Act & Assert
        try {
            assertThatThrownBy {
                context.publishEvent(
                    AfterLoadEvent(Document(), WorkflowDocument::class.java, "workflowDocument")
                )
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessage("boom")
        } finally {
            context.close()
        }
    }
}
