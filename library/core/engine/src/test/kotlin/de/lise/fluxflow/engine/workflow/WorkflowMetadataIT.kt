package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.workflow.WorkflowStarterService
import de.lise.fluxflow.api.workflow.WorkflowQueryService
import de.lise.fluxflow.springboot.testing.TestingConfiguration
import de.lise.fluxflow.stereotyped.metadata.Metadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [TestingConfiguration::class])
class WorkflowMetadataIT {

    @Autowired
    private var workflowStarterService: WorkflowStarterService? = null

    @Autowired
    private var workflowQueryService: WorkflowQueryService? = null

    @Test
    fun `workflow definition should contain metadata from workflow model annotations`() {
        // Arrange
        val workflowModel = TestWorkflowModelWithMetadata("Test Model")
        
        // Act
        val workflow = workflowStarterService!!.start(
            workflowModel,
            Continuation.none()
        )
        
        // Assert
        val metadata = workflow.definition.metadata
        assertThat(metadata).containsEntry("testMetadata", "test-value")
        assertThat(metadata).containsEntry("displayName", "Test Workflow")
        assertThat(metadata).hasSize(2)
    }
    
    @Test
    fun `workflow definition should contain metadata with multiple properties`() {
        // Arrange
        val workflowModel = TestWorkflowModelWithMultiplePropertyMetadata("Test Model")
        
        // Act
        val workflow = workflowStarterService!!.start(
            workflowModel,
            Continuation.none()
        )
        
        // Assert
        val metadata = workflow.definition.metadata
        assertThat(metadata).containsEntry("multiProp-first", "first-value")
        assertThat(metadata).containsEntry("multiProp-second", 42)
    }
}

@TestMetadata("test-value")
@DisplayName("Test Workflow")
data class TestWorkflowModelWithMetadata(val name: String)

@MultiPropertyMetadata(first = "first-value", second = 42)
data class TestWorkflowModelWithMultiplePropertyMetadata(val name: String)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Metadata("testMetadata")
annotation class TestMetadata(val value: String)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Metadata("displayName")
annotation class DisplayName(val value: String)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Metadata("multiProp")
annotation class MultiPropertyMetadata(
    val first: String,
    val second: Int
)