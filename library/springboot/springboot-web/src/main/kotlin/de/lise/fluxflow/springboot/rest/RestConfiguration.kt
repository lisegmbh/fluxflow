package de.lise.fluxflow.springboot.rest

import com.fasterxml.jackson.databind.JsonNode
import de.lise.fluxflow.api.ExperimentalApi
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.step.stateful.data.Data
import de.lise.fluxflow.api.step.stateful.data.StepDataService
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.rest.mapping.FromDtoConverter
import de.lise.fluxflow.rest.mapping.Mapping
import de.lise.fluxflow.rest.mapping.ToDtoConverter
import de.lise.fluxflow.rest.workflow.WorkflowDto
import de.lise.fluxflow.rest.workflow.step.StepDto
import de.lise.fluxflow.rest.workflow.step.data.StepDataDto
import de.lise.fluxflow.springboot.rest.workflow.WorkflowController
import de.lise.fluxflow.springboot.rest.workflow.step.WorkflowStepController
import de.lise.fluxflow.springboot.rest.workflow.step.data.StepDataController
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@ExperimentalApi
@Configuration
@Import(MappingConfiguration::class)
open class RestConfiguration {
    @Bean
    open fun workflowController(
        mapping: Mapping<Workflow<*>, WorkflowDto>,
        workflowService: WorkflowService
    ): WorkflowController {
        return WorkflowController(
            mapping,
            workflowService,
        )
    }

    @Bean
    open fun stepController(
        stepMapping: Mapping<Step, StepDto>,
        workflowService: WorkflowService,
        stepService: StepService
    ): WorkflowStepController {
        return WorkflowStepController(
            stepMapping,
            workflowService,
            stepService
        )
    }

    @Bean
    open fun stepDataController(
        stepDataMapping: Mapping<Data<*>, StepDataDto>,
        toDtoConverter: ToDtoConverter<Any, Any>,
        fromDtoConverter: FromDtoConverter<JsonNode>,
        workflowService: WorkflowService,
        stepService: StepService,
        stepDataService: StepDataService,
    ): StepDataController {
        return StepDataController(
            stepDataMapping = stepDataMapping,
            toDtoConverter = toDtoConverter,
            fromDtoConverter = fromDtoConverter,
            workflowService = workflowService,
            stepService = stepService,
            stepDataService = stepDataService,
        )
    }

    @Bean
    open fun controllerAdvice(): CommonExceptionControllerAdvice {
        return CommonExceptionControllerAdvice()
    }
}