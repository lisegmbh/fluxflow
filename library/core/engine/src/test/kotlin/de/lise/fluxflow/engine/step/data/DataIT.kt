package de.lise.fluxflow.engine.step.data

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.api.step.stateful.data.ModifiableData
import de.lise.fluxflow.api.step.stateful.data.StepDataService
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.engine.IntegrationTestConfig
import de.lise.fluxflow.springboot.testing.TestingConfiguration
import de.lise.fluxflow.stereotyped.step.bind
import de.lise.fluxflow.stereotyped.step.data.DataKindInspector
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [
        TestingConfiguration::class,
        IntegrationTestConfig::class
    ]
)
class DataIT {
    @Autowired
    lateinit var workflowService: WorkflowService

    @Autowired
    lateinit var stepService: StepService

    @Autowired
    lateinit var stepDataService: StepDataService

    private fun createTestMetadata(
        dataTestStep: DataTestStep,
        dataKind: DataKind,
    ): Map<String, Any> {
        val workflow = workflowService.start(
            Any(),
            Continuation.step(dataTestStep)
        )
        val step = stepService.findSteps(workflow).first() as StatefulStep
        return step.data
            .first { it.definition.kind == dataKind }
            .definition
            .metadata
    }

    @Test
    fun `data obtained from imported POJOs should be activateable`() {
        // Arrange 
        val stepModel = DataTestStepWithImportedData(
            ImportableData(
                "test firstname",
                ""
            )
        )

        // Act
        val workflow = workflowService.start(
            Any(),
            Continuation.step(stepModel)
        )
        val step = stepService.findSteps(workflow).first() as StatefulStep

        // Assert
        val firstname = step.data.first {
            it.definition.kind == DataKindInspector.getDataKind(ImportableData::firstname)
        }
        assertThat(firstname).isNotNull
        assertThat(firstname.get()).isEqualTo("test firstname")

        val lastname = step.data.last {
            it.definition.kind == DataKindInspector.getDataKind(ImportableData::lastname)
        }.let {
            @Suppress("UNCHECKED_CAST")
            it as ModifiableData<String>
        }
        assertThat(lastname).isNotNull
        assertThat(lastname.get()).isEqualTo("")
        lastname.set("new lastname")
        assertThat(lastname.get()).isEqualTo("new lastname")
    }

    @Test
    fun `data obtained from imports should be persistable and restorable from persistence layer`() {
        // Arrange
        val stepModel = DataTestStepWithImportedData(
            ImportableData(
                "firstname",
                "lastname"
            )
        )
        val workflow = workflowService.start(
            Any(),
            Continuation.step(stepModel)
        )
        val step = stepService.findSteps(workflow).first() as StatefulStep
        val lastname = step.data.last {
            it.definition.kind == DataKindInspector.getDataKind(ImportableData::lastname)
        }.let {
            @Suppress("UNCHECKED_CAST")
            it as ModifiableData<String>
        }

        // Act
        stepDataService.setValue(
            lastname,
            "new lastname"
        )
        val restoredStep = stepService.findStep(
            workflowService.get<Any>(
                workflow.identifier
            ),
            step.identifier
        )!!

        // Assert
        val restoredFirstname = stepDataService.getData<String>(
            restoredStep,
            DataKindInspector.getDataKind(ImportableData::firstname)
        )!!
        assertThat(restoredFirstname.get()).isEqualTo("firstname")

        val restoredLastname = stepDataService.getData<String>(
            restoredStep,
            DataKindInspector.getDataKind(ImportableData::lastname)
        )!!
        assertThat(restoredLastname.get()).isEqualTo("new lastname")
    }
    
    @Test
    fun `listeners defined on the importing type should be invoked whenever an imported data object changes`() {
        // Arrange
        val stepModel = DataTestStepWithImportedData(
            ImportableData(
                "firstname",
                "lastname"
            )
        )
        val workflow = workflowService.start(
            Any(),
            Continuation.step(stepModel)
        )
        val step = stepService.findSteps(workflow).first() as StatefulStep
        val lastname = step.data.last {
            it.definition.kind == DataKindInspector.getDataKind(ImportableData::lastname)
        }.let {
            @Suppress("UNCHECKED_CAST")
            it as ModifiableData<String>
        }

        // Act
        stepDataService.setValue(
            lastname,
            "new lastname"
        )
        
        // Assert
        val currentStep = step.bind<DataTestStepWithImportedData>()!!
        assertThat(currentStep.onChangeInvoked).isTrue()
    }

    @Test
    fun `listeners defined on the imported type should be invoked whenever the unwrapped data object changes`() {
        // Arrange
        val stepModel = DataTestStepWithImportedData(
            ImportableData(
                "firstname",
                "lastname"
            )
        )
        val workflow = workflowService.start(
            Any(),
            Continuation.step(stepModel)
        )
        val step = stepService.findSteps(workflow).first() as StatefulStep
        val lastname = step.data.last {
            it.definition.kind == DataKindInspector.getDataKind(ImportableData::lastname)
        }.let {
            @Suppress("UNCHECKED_CAST")
            it as ModifiableData<String>
        }

        // Act
        stepDataService.setValue(
            lastname,
            "new lastname"
        )

        // Assert
        val currentStep = step.bind<DataTestStepWithImportedData>()!!
        val dataWrapper = currentStep.data
        assertThat(dataWrapper.onChangeInvoked).isTrue()
        assertThat(dataWrapper.givenStep).isEqualTo(step)
        assertThat(dataWrapper.givenWorkflow).isEqualTo(step.workflow)
    }

    @Test
    fun `data should carry metadata from backing field`() {
        // Act
        val metadata = createTestMetadata(
            DataTestStep(),
            DataKind(DataTestStep::someDataWithFieldMetadata.name)
        )

        // Assert
        assertThat(metadata).containsExactly(
            *mapOf("key" to "field").entries.toTypedArray()
        )
    }

    @Test
    fun `data should carry metadata from property`() {
        // Act
        val metadata = createTestMetadata(
            DataTestStep(),
            DataKind(DataTestStep::someDataWithPropertyMetadata.name)
        )

        // Assert
        assertThat(metadata).containsExactly(
            *mapOf("key" to "property").entries.toTypedArray()
        )
    }

    @Test
    fun `data should carry metadata from getter`() {
        // Act
        val metadata = createTestMetadata(
            DataTestStep(),
            DataKind(DataTestStep::someDataWithGetterMetadata.name)
        )

        // Assert
        assertThat(metadata).containsExactly(
            *mapOf("key" to "getter").entries.toTypedArray()
        )
    }
}