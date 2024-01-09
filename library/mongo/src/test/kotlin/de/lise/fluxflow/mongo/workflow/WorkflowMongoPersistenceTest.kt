package de.lise.fluxflow.mongo.workflow

import de.lise.fluxflow.persistence.workflow.WorkflowData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class WorkflowMongoPersistenceTest {

    @Test
    fun `save should determine the model type and persist it`() {
        // Arrange
        val workflowRepository = mock<WorkflowRepository> {
            on { save(any<WorkflowDocument>()) }.doAnswer { inv ->
                inv.arguments.first() as WorkflowDocument
            }
        }
        val workflowMongoPersistence = WorkflowMongoPersistence(
            workflowRepository,
        )
        val testWorkflow = WorkflowData(
            "test-id",
            TestClass()
        )

        // Act
        workflowMongoPersistence.save(testWorkflow)

        // Assert
        val captor = argumentCaptor<WorkflowDocument>()
        verify(workflowRepository, times(1)).save(captor.capture())
        val persistedDocument = captor.firstValue
        assertThat(persistedDocument.modelType).isEqualTo(TestClass::class.qualifiedName)
    }

    @Test
    fun `save should set the model type to null if the model is null`() {
        // Arrange
        val workflowRepository = mock<WorkflowRepository> {
            on { save(any<WorkflowDocument>()) }.doAnswer { inv ->
                inv.arguments.first() as WorkflowDocument
            }
        }
        val workflowMongoPersistence = WorkflowMongoPersistence(
            workflowRepository,
        )
        val testWorkflow = WorkflowData(
            "test-id",
            null
        )

        // Act
        workflowMongoPersistence.save(testWorkflow)

        // Assert
        val captor = argumentCaptor<WorkflowDocument>()
        verify(workflowRepository, times(1)).save(captor.capture())
        val persistedDocument = captor.firstValue
        assertThat(persistedDocument.modelType).isNull()
    }

    private class TestClass
}