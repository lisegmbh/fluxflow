package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.step.*
import de.lise.fluxflow.api.step.stateful.StepActivationException
import de.lise.fluxflow.api.versioning.Version
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.engine.step.definition.StepDefinitionService
import de.lise.fluxflow.persistence.step.StepData
import org.slf4j.LoggerFactory

/**
 * The [RestoringStepActivationService] is a decorator for other [StepActivationService]s,
 * that will catch occurring exceptions
 * and tries to mitigate them by alternatively trying to restore the step using a pre-persisted step definition.
 *
 * @param baseStepActivationService The basic activation service to be decorated.
 * @param stepDefinitionService The service to be used to obtain pre-persisted step definitions.
 * @param handleAnyException If set to `false`, this service will only handle [StepActivationException]s.
 * Otherwise, it will try to catch and handle any caught [Exception].
 */
class RestoringStepActivationService(
    private val baseStepActivationService: StepActivationService,
    private val stepDefinitionService: StepDefinitionService,
    private val handleAnyException: Boolean,
) : StepActivationService {
    override fun <TWorkflowModel> activateInitial(
        workflow: Workflow<TWorkflowModel>,
        invokableStepDefinition: InvokableStepDefinition,
        identifier: StepIdentifier,
    ): Step {
        return baseStepActivationService.activateInitial(workflow, invokableStepDefinition, identifier)
    }

    override fun <TWorkflowModel> activateFromPersistence(
        workflow: Workflow<TWorkflowModel>,
        stepData: StepData,
    ): Step {
        return try {
            baseStepActivationService.activateFromPersistence(
                workflow,
                stepData
            )
        } catch (e: StepActivationException) {
            activateStepByRestoringFromPersistedDefinition(
                workflow,
                stepData,
                e
            )
        } catch (e: Exception) {
            if (handleAnyException) {
                activateStepByRestoringFromPersistedDefinition(
                    workflow,
                    stepData,
                    e
                )
            } else {
                throw e
            }
        }
    }

    private fun <TWorkflowModel> activateStepByRestoringFromPersistedDefinition(
        workflow: Workflow<TWorkflowModel>,
        stepData: StepData,
        cause: Exception,
    ): Step {
        Logger.warn(
            "Trying to restore step #{} using a persisted step definition, " +
                "because the following error occurred during activation.",
            stepData.id,
            cause
        )
        val restoredStepDefinition = stepDefinitionService.createRestoredStepDefinition(
            StepKind(stepData.kind),
            Version.parse(stepData.version),
            stepData
        ) ?: throw StepActivationException(
            "Could not restore step #${stepData.id}, as there is no persisted record of its definition.",
            cause
        )

        val version = Version.parse(stepData.version)

        val step = restoredStepDefinition.toInvokableStepDefinition().activate(
            StepActivationContext(
                workflow,
                StepIdentifier(stepData.id),
                version,
                stepData.status,
                stepData.metadata,
            )
        )
        Logger.debug(
            "Successfully restored step #{}.",
            step.identifier.value
        )
        return step
    }

    override fun toStepDefinition(definitionObject: Any): StepDefinition {
        return baseStepActivationService.toStepDefinition(definitionObject)
    }

    override fun toInvokableStepDefinition(definitionObject: Any): InvokableStepDefinition {
        return baseStepActivationService.toInvokableStepDefinition(definitionObject)
    }

    private companion object {
        val Logger = LoggerFactory.getLogger(RestoringStepActivationService::class.java)!!
    }
}

