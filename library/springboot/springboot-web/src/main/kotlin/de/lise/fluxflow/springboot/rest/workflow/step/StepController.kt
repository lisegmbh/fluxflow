package de.lise.fluxflow.springboot.rest.workflow.step

import de.lise.fluxflow.api.ExperimentalApi
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.rest.mapping.Mapping
import de.lise.fluxflow.rest.mapping.Mapping.Companion.mapWith
import de.lise.fluxflow.rest.workflow.step.StepDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ExperimentalApi
@RestController
@RequestMapping($$"${fluxflow.api.prefix:/api}/workflow/{workflowId}/step")
class StepController(
    private val stepMapping: Mapping<Step, StepDto>,
    private val workflowService: WorkflowService,
    private val stepService: StepService,
) {
    @GetMapping
    fun getAll(
      @PathVariable workflowId: String,
      filter: StepFilter
    ): List<StepDto> {
        val workflow = workflowService.get<Any?>(
            WorkflowIdentifier(workflowId)
        )

        return stepService.findSteps(workflow)
            .mapWith(stepMapping)
    }

    @GetMapping("/{stepId}")
    fun getById(
        @PathVariable workflowId: String,
        @PathVariable stepId: String
    ): StepDto {
        val workflowIdentifier = WorkflowIdentifier(workflowId)
        val workflow = workflowService.get<Any?>(
            workflowIdentifier
        )

        val stepIdentifier = StepIdentifier(stepId)
        return stepService.findStep(
            workflow,
            StepIdentifier(stepId)
        )?.mapWith(stepMapping)
            ?: throw StepNotFoundException(
                workflowIdentifier,
                stepIdentifier
            )
    }
}