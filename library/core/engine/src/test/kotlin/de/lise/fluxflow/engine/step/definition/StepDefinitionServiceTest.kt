package de.lise.fluxflow.engine.step.definition

import de.lise.fluxflow.api.step.StepDefinition
import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.step.stateful.StatefulStepDefinition
import de.lise.fluxflow.api.step.stateful.data.DataDefinition
import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.api.versioning.SimpleVersion
import de.lise.fluxflow.persistence.step.definition.StepDefinitionData
import de.lise.fluxflow.persistence.step.definition.StepDefinitionPersistence
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class StepDefinitionServiceTest {
    @Test
    fun `save should call the persistence layer with an empty data list for stateless steps`() {
        // Arrange
        val definition = mock<StepDefinition> {
            on { kind } doReturn StepKind("test")
            on { version } doReturn SimpleVersion("test")
            on { metadata } doReturn mapOf<String, Any>("test" to true)
        }
        val captor = argumentCaptor<StepDefinitionData> {}
        val persistence = mock<StepDefinitionPersistence> {}
        val service = StepDefinitionService(persistence)

        // Act
        service.save(definition)

        // Assert
        verify(persistence, times(1)).save(captor.capture())
        val persistedData = captor.firstValue
        assertThat(persistedData.version).isEqualTo(definition.version.version)
        assertThat(persistedData.kind).isEqualTo(definition.kind.value)
        assertThat(persistedData.metadata).isEqualTo(definition.metadata)
        assertThat(persistedData.data).isEmpty()
    }

    @Test
    fun `save should call the persistence layer with mapped data entries for stateful steps`() {
        // Arrange
        val dataDefinition = mock<DataDefinition<String>> {
            on { kind } doReturn DataKind("test")
            on { type } doReturn String::class.java
        }
        val definition = mock<StatefulStepDefinition> {
            on { kind } doReturn StepKind("test")
            on { version } doReturn SimpleVersion("test")
            on { metadata } doReturn mapOf<String, Any>("test" to true)
            on { data } doReturn listOf(dataDefinition)
        }
        val captor = argumentCaptor<StepDefinitionData> {}
        val persistence = mock<StepDefinitionPersistence> {}
        val service = StepDefinitionService(persistence)

        // Act
        service.save(definition)

        // Assert
        verify(persistence, times(1)).save(captor.capture())
        val persistedData = captor.firstValue
        assertThat(persistedData.data).hasSize(1)
        
        val dataDefinitionData = persistedData.data.first()
        assertThat(dataDefinitionData.kind).isEqualTo(dataDefinition.kind.value)
        assertThat(dataDefinitionData.type).isEqualTo(String::class.java.typeName)
    }
}