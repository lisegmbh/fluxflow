package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.ioc.IocProvider
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepDefinition
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.step.stateful.StepActivationException
import de.lise.fluxflow.api.step.stateful.data.ModifiableData
import de.lise.fluxflow.api.versioning.CompatibilityTester
import de.lise.fluxflow.api.versioning.Version
import de.lise.fluxflow.api.versioning.VersionCompatibility
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.stereotyped.step.StepDefinitionBuilder

class DefaultStepActivationService(
    private val iocProvider: IocProvider,
    private val stepDefinitionBuilder: StepDefinitionBuilder,
    private val stepTypeResolver: StepTypeResolver,
    private val requiredCompatibility: VersionCompatibility,
    private val compatibilityTester: CompatibilityTester
) : StepActivationService {
    override fun <TWorkflowModel> activate(workflow: Workflow<TWorkflowModel>, stepData: StepData): Step {
        val stepDefinition = activeStepDefinition(workflow, stepData)
        val persistedVersion = Version.parse(stepData.version)
        val currentCompatibility = compatibilityTester.checkCompatibility(persistedVersion, stepDefinition.version)

        if(!requiredCompatibility.isSatisfiedBy(currentCompatibility)) {
            throw StepActivationException(
                "The step #${stepData.id} failed activation, because it doesn't satisfy the required version compatibility (" +
                        "persisted version: ${persistedVersion.version}, " +
                        "current version: ${stepDefinition.version.version}, " +
                        "actual compatibility: $currentCompatibility, " +
                        "required compatibility: $requiredCompatibility)"
            )
        }
        
        val step = stepDefinition.createStep(
            workflow,
            StepIdentifier(stepData.id),
            persistedVersion,
            stepData.status,
            stepData.metadata
        )
        if (step is StatefulStep) {
            step.data
                .filterIsInstance<ModifiableData<Any?>>()
                .filter { !it.definition.isCalculatedValue }
                .forEach{
                    if (stepData.data.containsKey(it.definition.kind.value)) {
                        it.set(stepData.data[it.definition.kind.value])
                    }
                }
        }

        return step
    }

    override fun toStepDefinition(definitionObject: Any): StepDefinition {
        return definitionObject
            .takeIf { it is StepDefinition }
            ?.let { it as StepDefinition }
            ?: stepDefinitionBuilder.build(definitionObject)
    }

    private fun <TWorkflowModel> activeStepDefinition(
        workflow: Workflow<TWorkflowModel>,
        stepData: StepData
    ): StepDefinition {
        return StepActivation(
            stepTypeResolver,
            stepDefinitionBuilder,
            iocProvider,
            workflow,
            stepData
        ).activate()
    }
}