package de.lise.fluxflow.springboot.rest.workflow.step

import de.fluxflow.flowquery.expression.PredicateExpression
import de.fluxflow.flowquery.mapper.expression.ExpressionMapper
import de.fluxflow.flowquery.query.FlowQuery
import de.lise.fluxflow.api.ExperimentalApi
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.step.query.StepQueryable
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.rest.mapping.Mapping
import de.lise.fluxflow.rest.mapping.Mapping.Companion.mapWith
import de.lise.fluxflow.rest.workflow.step.StatefulStepSpecDto
import de.lise.fluxflow.rest.workflow.step.StepDto
import de.lise.fluxflow.springboot.rest.filter.FilterParser
import de.lise.fluxflow.springboot.rest.filter.ODataTreeBuilder
import org.springframework.web.bind.annotation.*

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
      @RequestParam(
          $$"$filter",
          required = false
      )
      filter: String?
    ): List<StepDto> {
        val workflow = workflowService.get<Any?>(
            WorkflowIdentifier(workflowId),
        )

        val filterParser = FilterParser(
            StepDto::class,
            ODataTreeBuilder(
                StepDto::class,
                setOf(StatefulStepSpecDto::class),
            )
        )

        val parsedFilter = filter?.let {
            filterParser.parse(it)
        }

        val replacer: ExpressionMapper = StepDtoToStepQueryableReplacer().toMapper()

        val replacedFilter = parsedFilter?.let {
            replacer.map(it) as PredicateExpression<StepQueryable>
        }

        val query: FlowQuery<StepQueryable, StepQueryable>? = replacedFilter?.let {
            FlowQuery.of<StepQueryable>().where(
                replacedFilter
            )
        }

        val results = query?.let {
            stepService.findAll(workflow, query).items
        } ?: stepService.findSteps(workflow)



//        stepService.findAll(
//            workflow,
//            query
//        ).mapWith(stepMapping)
        return results.mapWith(stepMapping)
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