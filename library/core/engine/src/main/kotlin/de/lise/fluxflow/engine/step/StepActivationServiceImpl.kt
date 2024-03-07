package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.ioc.IocProvider
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepDefinition
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.step.stateful.data.ModifiableData
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.stereotyped.step.StepDefinitionBuilder

class StepActivationServiceImpl(
    private val iocProvider: IocProvider,
    private val stepDefinitionBuilder: StepDefinitionBuilder,
    private val stepTypeResolver: StepTypeResolver
) : StepActivationService {
    override fun <TWorkflowModel> activate(workflow: Workflow<TWorkflowModel>, stepData: StepData): Step {
        val step = activeStepDefinition(workflow, stepData).createStep(
            workflow,
            StepIdentifier(stepData.id),
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