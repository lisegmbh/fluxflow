package de.lise.fluxflow.engine.continuation

import de.lise.fluxflow.api.ReferredWorkflowObject
import de.lise.fluxflow.api.WorkflowObjectReference
import de.lise.fluxflow.api.continuation.*
import de.lise.fluxflow.api.job.JobService
import de.lise.fluxflow.api.job.continuation.JobCancellationContinuation
import de.lise.fluxflow.api.job.continuation.JobContinuation
import de.lise.fluxflow.api.step.continuation.StepContinuation
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.api.workflow.WorkflowStarterService
import de.lise.fluxflow.api.workflow.continuation.ForkBehavior
import de.lise.fluxflow.api.workflow.continuation.WorkflowContinuation
import de.lise.fluxflow.engine.continuation.history.ContinuationHistoryServiceImpl
import de.lise.fluxflow.engine.step.StepActivationService
import de.lise.fluxflow.engine.step.StepServiceImpl

open class ContinuationService(
    private val stepService: StepServiceImpl,
    private val stepActivationService: StepActivationService,
    private val jobService: JobService,
    private val continuationHistoryService: ContinuationHistoryServiceImpl,
    private val workflowStarterService: WorkflowStarterService,
    private val workflowService: WorkflowService
) {
    /**
     * Executes the given continuation.
     * @param workflow The workflow to be continued.
     * @param originatingObject The originating object that produced the given [continuation].
     * Might be `null`, if the continuation has been crated from a job or is the initial continuation.
     * @param continuation The continuation to be executed.
     */
    open fun execute(
        workflow: Workflow<*>,
        originatingObject: ReferredWorkflowObject<*>?,
        continuation: Continuation<*>,
        stepFinalizationSemaphore: StepFinalizationSemaphore = StepFinalizationSemaphore(false),
    ) {
        val originatingStep = originatingObject?.step
        if (originatingStep != null) {
            when (continuation.statusBehavior) {
                StatusBehavior.Complete -> stepFinalizationSemaphore.finalizeStep {
                    stepService.complete(originatingStep)
                }

                StatusBehavior.Cancel -> stepFinalizationSemaphore.finalizeStep {
                    stepService.cancel(originatingStep)
                }

                StatusBehavior.Preserve -> {}
            }
        }

        when (continuation) {
            is NoContinuation -> {
                return executeNothing(workflow, originatingObject, continuation)
            }

            is StepContinuation<*> -> {
                return executeNextStep(workflow, originatingObject, continuation)
            }

            is JobContinuation<*> -> {
                return executeJob(workflow, originatingObject, continuation)
            }

            is JobCancellationContinuation -> {
                return executeJobCancellation(workflow, originatingObject, continuation)
            }

            is MultipleContinuation -> {
                return executeAll(workflow, originatingObject, continuation, stepFinalizationSemaphore)
            }

            is RollbackContinuation -> {
                return executeRollback(workflow, originatingObject, continuation)
            }

            is WorkflowContinuation<*, *> -> {
                return executeWorkflow(workflow, continuation)
            }
        }
    }

    private fun executeWorkflow(
        workflow: Workflow<*>,
        continuation: WorkflowContinuation<*, *>
    ) {
        if (continuation.forkBehavior == ForkBehavior.Replace) {
            val oldId = workflow.id
            workflowService.replace(oldId)
            workflowStarterService.start(
                continuation.model,
                continuation.initialWorkflowContinuation,
                null,
                oldId
            )
        } else {
            workflowStarterService.start(
                continuation.model,
                continuation.initialWorkflowContinuation,
                null
            )
            if (continuation.forkBehavior == ForkBehavior.Remove) {
                workflowService.delete(workflow.id)
            }
        }
    }

    private fun executeAll(
        workflow: Workflow<*>,
        originatingObject: ReferredWorkflowObject<*>?,
        continuation: MultipleContinuation,
        semaphore: StepFinalizationSemaphore
    ) {
        continuation.continuations.forEach { nestedContinuation ->
            execute(
                workflow,
                originatingObject,
                nestedContinuation,
                semaphore
            )
        }
    }

    private fun executeJob(
        workflow: Workflow<*>,
        originatingObject: ReferredWorkflowObject<*>?,
        continuation: JobContinuation<*>
    ) {
        val job = jobService.schedule(workflow, continuation)
        continuationHistoryService.create(
            workflow.id,
            originatingObject?.reference,
            continuation,
            WorkflowObjectReference.create(job)
        )
    }

    private fun executeJobCancellation(
        workflow: Workflow<*>,
        originatingObject: ReferredWorkflowObject<*>?,
        continuation: JobCancellationContinuation
    ) {
        jobService.cancelAll(
            workflow,
            continuation.cancellationKey
        )
        continuationHistoryService.create(
            workflow.id,
            originatingObject?.reference,
            continuation,
            null
        )
    }

    private fun executeRollback(
        workflow: Workflow<*>,
        originatingObject: ReferredWorkflowObject<*>?,
        continuation: RollbackContinuation
    ) {
        val currentStep = originatingObject?.step ?: throw InvalidContinuationException(
            "Rollback continuation can only be used within workflow steps"
        )
        val previousStep =
            continuationHistoryService.findPreviousStep(currentStep) ?: throw InvalidContinuationException(
                "The step \"${currentStep.identifier.value}\" of workflow \"${workflow.id.value}\" does not have a previous step"
            )

        val reactivatedStep = stepService.reactivate(previousStep)

        continuationHistoryService.create(
            workflow.id,
            originatingObject.reference,
            continuation,
            ReferredWorkflowObject.create(reactivatedStep).reference
        )
    }

    private fun executeNothing(
        workflow: Workflow<*>,
        originatingObject: ReferredWorkflowObject<*>?,
        continuation: NoContinuation
    ) {
        continuationHistoryService.create(
            workflow.id,
            originatingObject?.reference,
            continuation,
            null
        )
    }

    private fun executeNextStep(
        workflow: Workflow<*>,
        originatingObject: ReferredWorkflowObject<*>?,
        continuation: StepContinuation<*>,
    ) {
        val model = continuation.model!!
        val stepDefinition = stepActivationService.toStepDefinition(model)

        val creationResult = stepService.create(workflow, stepDefinition)
        val nextOriginatingObject = ReferredWorkflowObject.create(creationResult.step)

        continuationHistoryService.create(
            workflow.id,
            originatingObject?.reference,
            continuation,
            nextOriginatingObject.reference
        )

        val nextStepFinalizationSemaphore = StepFinalizationSemaphore(false)
        creationResult.nextContinuations.forEach {
            execute(
                workflow,
                nextOriginatingObject,
                it,
                nextStepFinalizationSemaphore
            )
        }
    }
}