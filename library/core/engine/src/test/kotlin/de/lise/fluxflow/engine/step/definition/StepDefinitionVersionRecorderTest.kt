package de.lise.fluxflow.engine.step.definition

import de.lise.fluxflow.api.step.StepDefinition
import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.versioning.SimpleVersion
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class StepDefinitionVersionRecorderTest {
    @Test
    fun `persistence should not be invoked when re-recording the same definition`() {
        // Arrange
        val persistence = mock<StepDefinitionService> {}
        val definition = mock<StepDefinition> {
            on { kind } doReturn StepKind("test")
            on { version } doReturn SimpleVersion("test")
        }
                
        val recorder = StepDefinitionVersionRecorder(persistence)
        
        // Act
        recorder.record(definition)
        // the second invocation is intentional and part of the test
        recorder.record(definition) 
        
        // Assert 
        verify(persistence, times(1)).save(any())
    }
    
    @Test
    fun `persistence should be invoked for each version`() {
        // Arrange
        val persistence = mock<StepDefinitionService> {}
        val v1 = mock<StepDefinition> {
            on { kind } doReturn StepKind("test")
            on { version } doReturn SimpleVersion("test-1")
        }
        val v2 = mock<StepDefinition> {
            on { kind } doReturn StepKind("test")
            on { version } doReturn SimpleVersion("test-2")
        }

        val recorder = StepDefinitionVersionRecorder(persistence)

        // Act
        recorder.record(v1)
        // the second invocation is intentional and part of the test
        recorder.record(v2)

        // Assert 
        verify(persistence, times(2)).save(any())
    }
    
}