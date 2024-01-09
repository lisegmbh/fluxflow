package de.lise.fluxflow.springboot.activation.parameter

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.step.stateful.action.ActionService
import de.lise.fluxflow.api.workflow.WorkflowStarterService
import de.lise.fluxflow.springboot.testing.TestingConfiguration
import de.lise.fluxflow.stereotyped.step.ReflectedStatefulStepDefinition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [
        TestingConfiguration::class,
        TestComponentWithValues::class,
        ValueTestBeanConfiguration::class
    ],
    properties = [
        "props.string=a test value",
        "props.int=42",
        "props.someString=A VALUE"
    ]
)
class SpringValueExpressionParameterResolverIT {

    @Autowired
    var testComponentWithValues: TestComponentWithValues? = null

    @Autowired
    var workflowStarterService: WorkflowStarterService? = null

    @Autowired
    var actionService: ActionService? = null

    @Autowired
    var stepService: StepService? = null

    private fun prepareTestStep(): TestStepWithValues {
        val workflow = workflowStarterService!!.start(
            Any(),
            Continuation.step(TestStepWithValues())
        )
        val step = stepService!!.findSteps(workflow).first() as StatefulStep
        val stepModel = (step.definition as ReflectedStatefulStepDefinition).instance as TestStepWithValues
        actionService!!.invokeAction(step.actions.first())
        return stepModel
    }

    @Test
    fun `@Value should support simple properties`() {
        // Arrange & Act
        val stepModel = prepareTestStep()

        // Assert
        assertThat(stepModel.aStringValue)
            .isEqualTo(testComponentWithValues!!.aStringValue)
            .isEqualTo("a test value")
    }

    @Test
    fun `@Value should support properties and ignored default if value is set`() {
        // Arrange & Act
        val stepModel = prepareTestStep()

        // Assert
        assertThat(stepModel.aStringValueWithNeedlessDefault)
            .isEqualTo(testComponentWithValues!!.aStringValueWithNeedlessDefault)
            .isEqualTo("a test value")
    }

    @Test
    fun `@Value should support properties and used the default if value is missing`() {
        // Arrange & Act
        val stepModel = prepareTestStep()

        // Assert
        assertThat(stepModel.aStringValueWithDefault)
            .isEqualTo(testComponentWithValues!!.aStringValueWithDefault)
            .isEqualTo("i am a default")
    }

    @Test
    fun `@Value should support properties requiring type conversion`() {
        // Arrange & Act
        val stepModel = prepareTestStep()

        // Assert
        assertThat(stepModel.aIntValue)
            .isEqualTo(testComponentWithValues!!.aIntValue)
            .isEqualTo(42)
    }

    @Test
    fun `@Value should support Spring Expression Language`() {
        // Arrange & Act
        val stepModel = prepareTestStep()
        
        // Assert
        assertThat(stepModel.now)
            .isEqualTo(testComponentWithValues!!.now)
            .isEqualTo(ValueTestBeanConfiguration.TestInstant)
    }

    @Test
    fun `@Value should support mixed Spring Expression Language and properties`() {
        // Arrange & Act
        val stepModel = prepareTestStep()

        // Assert
        assertThat(stepModel.someValueIsEqual)
            .isEqualTo(testComponentWithValues!!.someValueIsEqual)
            .isTrue()
    }
}

