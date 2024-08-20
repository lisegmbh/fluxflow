package de.lise.fluxflow.stereotyped.step

import de.lise.fluxflow.api.step.stateful.StatefulStepDefinition
import de.lise.fluxflow.api.step.stateful.data.DataDefinition
import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.api.versioning.NoVersion
import de.lise.fluxflow.stereotyped.job.Job
import de.lise.fluxflow.stereotyped.metadata.MetadataBuilder
import de.lise.fluxflow.stereotyped.step.action.ActionDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.automation.Automated
import de.lise.fluxflow.stereotyped.step.automation.OnCreated
import de.lise.fluxflow.stereotyped.step.automation.Trigger
import de.lise.fluxflow.stereotyped.step.data.DataDefinitionBuilder
import de.lise.fluxflow.stereotyped.versioning.VersionBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

@Suppress("unused")
class StepDefinitionBuilderTest {
    private val mockVersionBuilder = mock<VersionBuilder> {
        on { build(any()) } doReturn NoVersion()
    }

    @Test
    fun `build should not return null`() {
        // Arrange
        val builder = StepDefinitionBuilder(
            mockVersionBuilder,
            mock {},
            mock {
                on { buildDataDefinition<Any>(any()) } doReturn emptyList()
            },
            mock {},
            mock {},
            mutableMapOf()
        )

        // Act
        val result = builder.build(TestClass("", ""))

        // Assert
        assertThat(result).isNotNull
    }

    @Test
    fun `build should use the data build to get step data`() {
        // Arrange
        val dataBuilder = mockDataBuilder()
        val builder = StepDefinitionBuilder(
            mockVersionBuilder,
            mock {},
            dataBuilder,
            mock {},
            mock {},
            mutableMapOf(),
        )

        // Act
        val result = builder.build(TestClass("", "")) as StatefulStepDefinition

        // Assert
        verify(dataBuilder).buildDataDefinition(TestClass::class)
        assertThat(result.data).containsExactlyInAnyOrder(testStepDataDefinition)
    }

    @Test
    fun `build should not consider automated functions to be actions`() {
        // Arrange
        val actionDefinitionBuilder = mock<ActionDefinitionBuilder> {}
        val builder = StepDefinitionBuilder(
            mockVersionBuilder,
            actionDefinitionBuilder,
            mockDataBuilder(),
            mock {},
            mock {
                on { isAutomation(any()) } doReturn true
            },
            mutableMapOf()
        )

        // Act
        val result = builder.build(TestClassWithAutomation())

        // Assert
        verify(actionDefinitionBuilder, never()).build<Any>(any(), any())
        assertThat((result as StatefulStepDefinition).actions).isEmpty()
    }
    
    @Test
    fun `build should use the metadata builder to obtain the step metadata`() {
        // Arrange
        val testMetadata = mock<Map<String, Any>>()
        val metadataBuilder = mock<MetadataBuilder> {
            on { build(eq(TestClass::class)) } doReturn testMetadata
        }
        val builder = StepDefinitionBuilder(
            mockVersionBuilder,
            mock {},
            mockDataBuilder(),
            metadataBuilder,
            mock {},
            mutableMapOf()
        )
        
        // Act
        val result = builder.build(TestClass("stringProp", "modifiableProp"))
        
        // Assert
        verify(metadataBuilder, times(1)).build(TestClass::class)
        assertThat(result.metadata).isSameAs(testMetadata)
    }

    private val testStepDataDefinition = mock<DataDefinition<Any>> {
        on { kind }.doReturn(
            DataKind(
                "test"
            )
        )
    }
    
    private fun mockDataBuilder(): DataDefinitionBuilder {
        return mock<DataDefinitionBuilder> {
            on { buildDataDefinition<Any>(any()) }.doAnswer { answer ->
                listOf { _ -> testStepDataDefinition }
            }
        }
    }

    class TestClass(
        val readOnlyProp: String,
        var modifiableProp: String
    ) {}

    class TestClassWithPrivate(
        val publicField: String,
        private val privateField: String
    )

    @Job
    class SomeJob
    class TestClassWithJob(
        val publicField: String,
        val job: SomeJob
    )

    class TestClassWithAutomation {
        @Automated(Trigger.OnCreated)
        fun `automated function`() {
        }

        @OnCreated
        fun `onStarted function`() {
        }
    }
}