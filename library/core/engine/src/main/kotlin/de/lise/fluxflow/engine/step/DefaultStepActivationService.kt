package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.ioc.IocProvider
import de.lise.fluxflow.api.step.*
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.step.stateful.StepActivationException
import de.lise.fluxflow.api.step.stateful.data.ModifiableData
import de.lise.fluxflow.api.versioning.CompatibilityTester
import de.lise.fluxflow.api.versioning.Version
import de.lise.fluxflow.api.versioning.VersionCompatibility
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.stereotyped.step.ReflectedStatefulStepDefinition
import de.lise.fluxflow.stereotyped.step.StepDefinitionBuilder
import kotlin.reflect.full.isSubclassOf

class DefaultStepActivationService(
    private val iocProvider: IocProvider,
    private val stepDefinitionBuilder: StepDefinitionBuilder,
    private val stepTypeResolver: StepTypeResolver,
    private val requiredCompatibility: VersionCompatibility,
    private val compatibilityTester: CompatibilityTester,
) : StepActivationService {
    override fun <TWorkflowModel> activateInitial(
        workflow: Workflow<TWorkflowModel>,
        invokableStepDefinition: InvokableStepDefinition,
        identifier: StepIdentifier,
    ): Step {
        return activate(
            workflow,
            StepData(
                id = identifier.value,
                workflowId = workflow.identifier.value,
                kind = invokableStepDefinition.definition.kind.value,
                version = invokableStepDefinition.definition.version.version,
                emptyMap(),
                Status.Active,
                invokableStepDefinition.definition.metadata
            ),
            invokableStepDefinition
        )
    }

    override fun <TWorkflowModel> activateFromPersistence(
        workflow: Workflow<TWorkflowModel>,
        stepData: StepData,
    ): Step {
        val stepDefinition = toStepDefinition(
            workflow,
            stepData
        )
        val invokableStepDefinition = when (stepDefinition) {
            is ReflectedStatefulStepDefinition -> stepDefinition.toInvokableStepDefinition(
                StepSpecificInstanceActivation(
                    iocProvider,
                    workflow,
                    stepData
                ).activateInstance(
                    stepDefinition.backingType
                )
            )

            else -> throw StepActivationException(
                "Could not activate step '${stepData.id}' of kind '${stepDefinition.kind}'," +
                    " because the definition type ${stepDefinition::class.simpleName} is not supported."
            )

        }

        return activate(
            workflow,
            stepData,
            invokableStepDefinition
        )
    }

    private fun <TWorkflowModel> activate(
        workflow: Workflow<TWorkflowModel>,
        stepData: StepData,
        invokableStepDefinition: InvokableStepDefinition,
    ): Step {
        val persistedVersion = Version.parse(stepData.version)
        val currentCompatibility = compatibilityTester.checkCompatibility(
            persistedVersion,
            invokableStepDefinition.definition.version
        )

        if (!requiredCompatibility.isSatisfiedBy(currentCompatibility)) {
            throw StepActivationException(
                "The step #${stepData.id} failed activation, because it doesn't satisfy the required version compatibility (" +
                    "persisted version: ${persistedVersion.version}, " +
                    "current version: ${invokableStepDefinition.definition.version.version}, " +
                    "actual compatibility: $currentCompatibility, " +
                    "required compatibility: $requiredCompatibility)"
            )
        }
        val stepIdentifier = StepIdentifier(stepData.id)

        val step = invokableStepDefinition.activate(
            StepActivationContext(
                workflow,
                stepIdentifier,
                persistedVersion,
                stepData.status,
                stepData.metadata,
            )
        )

        if (step is StatefulStep) {
            step.data
                .filterIsInstance<ModifiableData<Any?>>()
                .filter { !it.definition.isCalculatedValue }
                .forEach {
                    if (stepData.data.containsKey(it.definition.kind.value)) {
                        it.set(stepData.data[it.definition.kind.value])
                    }
                }
        }

        return step
    }

    private fun <TWorkflowModel> toStepDefinition(
        workflow: Workflow<TWorkflowModel>,
        stepData: StepData,
    ): StepDefinition {
        val type = try {
            stepTypeResolver.resolveType(StepKind(stepData.kind))
        } catch (e: ClassNotFoundException) {
            throw StepActivationException(
                "Unable to activate step #${stepData.id} with kind '${stepData.kind}', " +
                    "because it's type could not be resolved/activated.",
                e
            )
        }

        if (type.isSubclassOf(StepDefinition::class)) {
            return StepSpecificInstanceActivation(
                iocProvider,
                workflow,
                stepData
            ).activateInstance(type) as StepDefinition
        }

        return stepDefinitionBuilder.build(type)
    }

    override fun toInvokableStepDefinition(definitionObject: Any): InvokableStepDefinition {
        return definitionObject
            .takeIf { it is InvokableStepDefinition }
            ?.let { it as InvokableStepDefinition }
            ?: stepDefinitionBuilder.build(definitionObject::class).toInvokableStepDefinition(definitionObject)
    }

    override fun toStepDefinition(definitionObject: Any): StepDefinition {
        return definitionObject
            .takeIf { it is StepDefinition }
            ?.let { it as StepDefinition }
            ?: stepDefinitionBuilder.build(definitionObject::class)
    }
}