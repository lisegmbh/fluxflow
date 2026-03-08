package de.lise.fluxflow.springboot.rest.workflow.step.data

import com.fasterxml.jackson.databind.JsonNode
import de.lise.fluxflow.api.ExperimentalApi
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.step.stateful.data.Data
import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.api.step.stateful.data.StepDataService
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.rest.mapping.FromDtoConverter
import de.lise.fluxflow.rest.mapping.Mapping
import de.lise.fluxflow.rest.mapping.Mapping.Companion.mapWith
import de.lise.fluxflow.rest.mapping.ToDtoConverter
import de.lise.fluxflow.rest.workflow.step.data.StepDataDto
import de.lise.fluxflow.springboot.rest.workflow.step.StepNotFoundException
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@ExperimentalApi
@RestController
@RequestMapping($$"${fluxflow.api.prefix:/api}/workflow/{workflowId}/step/{stepId}/data")
class StepDataController(
    private val stepDataMapping: Mapping<Data<*>, StepDataDto>,
    private val toDtoConverter: ToDtoConverter<Any, Any>,
    private val fromDtoConverter: FromDtoConverter<JsonNode>,
    private val workflowService: WorkflowService,
    private val stepService: StepService,
    private val stepDataService: StepDataService
) {
    @GetMapping
    fun getAll(
        @PathVariable workflowId: String,
        @PathVariable stepId: String,
    ): List<StepDataDto> {
        val step = getStep(workflowId, stepId)

        return stepDataService.getData(step).mapWith(stepDataMapping)
    }

    @GetMapping("/{kind}")
    fun getByKind(
        @PathVariable workflowId: String,
        @PathVariable stepId: String,
        @PathVariable kind: String
    ): StepDataDto {
        val step = getStep(workflowId, stepId)

        return stepDataService.getData<Any?>(
            step,
            DataKind(kind)
        ).mapWith(stepDataMapping)
    }

    @GetMapping("/{kind}/value", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getValueByKind(
        @PathVariable workflowId: String,
        @PathVariable stepId: String,
        @PathVariable kind: String
    ): Any? {
        val step = getStep(workflowId, stepId)

        return stepDataService.getData<Any?>(
            step,
            DataKind(kind)
        ).get()?.let {
            toDtoConverter.toDto(it)
        }
    }

    @PutMapping("/{kind}/value", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun setValueByKind(
        @PathVariable workflowId: String,
        @PathVariable stepId: String,
        @PathVariable kind: String,
        @RequestBody value: JsonNode
    ): Any? {
        val step = getStep(workflowId, stepId)
        val data = stepDataService.getData<Any?>(
            step,
            DataKind(kind)
        )
        val actualValue = fromDtoConverter.fromDto(
            value,
            data.definition.type
        )

        stepDataService.setValue(
            data,
            actualValue
        )

        return actualValue?.let {
            toDtoConverter.toDto(it)
        }
    }


    private fun getStep(
        workflowId: String,
        stepId: String
    ): Step {
        val workflowIdentifier = WorkflowIdentifier(workflowId)
        val workflow = workflowService.get<Any?>(workflowIdentifier)

        val stepIdentifier = StepIdentifier(stepId)
        return stepService.findStep(workflow, stepIdentifier) ?: throw StepNotFoundException(
            workflowIdentifier,
            stepIdentifier
        )
    }
}