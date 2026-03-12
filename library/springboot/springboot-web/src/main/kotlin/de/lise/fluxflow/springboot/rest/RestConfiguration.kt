package de.lise.fluxflow.springboot.rest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
import de.lise.fluxflow.springboot.rest.patch.JsonReplaceOperation
import de.lise.fluxflow.springboot.rest.patch.PatchAction
import de.lise.fluxflow.springboot.rest.patch.PatchCapability
import de.lise.fluxflow.springboot.rest.patch.PatchRegistry
import de.lise.fluxflow.springboot.rest.workflow.WorkflowController
import de.lise.fluxflow.springboot.rest.workflow.step.StepController
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
    ): StepController {
        return StepController(
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
        patchRegistry: PatchRegistry<Data<*>>
    ): StepDataController {
        return StepDataController(
            stepDataMapping = stepDataMapping,
            toDtoConverter = toDtoConverter,
            fromDtoConverter = fromDtoConverter,
            workflowService = workflowService,
            stepService = stepService,
            stepDataService = stepDataService,
            stepDataPatchRegistry = patchRegistry
        )
    }


    @Bean
    open fun patchReplaceStepDataValue(
        objectMapper: ObjectMapper,
        stepDataService: StepDataService,
    ): PatchCapability<Data<*>> {
        return PatchCapability.withDescription<Data<*>>("Replace on /value")
            .forOperationType<JsonReplaceOperation>()
            .forPath("/value")
            .build { original, op ->
                val updatedValue = objectMapper.convertValue<Any?>(
                    op.value,
                    objectMapper.typeFactory.constructType(original.definition.type)
                )
                PatchAction { data ->
                    stepDataService.setValue(data as Data<Any?>, updatedValue)
                    data
                }
            }
    }

    @Bean
    open fun stepDataPatchRegistry(
        capabilities: List<PatchCapability<Data<*>>>
    ): PatchRegistry<Data<*>> {
        return PatchRegistry(capabilities)
    }

    @Bean
    open fun controllerAdvice(): CommonExceptionControllerAdvice {
        return CommonExceptionControllerAdvice()
    }
}