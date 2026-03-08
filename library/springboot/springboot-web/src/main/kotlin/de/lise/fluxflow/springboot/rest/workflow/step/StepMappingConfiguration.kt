package de.lise.fluxflow.springboot.rest.workflow.step

import de.lise.fluxflow.api.ExperimentalApi
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.rest.mapping.Mapping
import de.lise.fluxflow.rest.mapping.Mapping.Companion.mapWith
import de.lise.fluxflow.rest.mapping.ToDtoConverter
import de.lise.fluxflow.rest.workflow.step.StepDto
import de.lise.fluxflow.rest.workflow.step.StepMetadataDto
import de.lise.fluxflow.rest.workflow.step.StepSpecDto
import de.lise.fluxflow.rest.workflow.step.StepStatusDto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ExperimentalApi
@Configuration
open class StepMappingConfiguration {
    @Bean
    open fun stepMapping(
        metadataMapping: Mapping<Step, StepMetadataDto>,
        specMapping: Mapping<Step, StepSpecDto>,
        statusMapping: Mapping<Step, StepStatusDto>,
    ): Mapping<Step, StepDto> {
        return Mapping {
            DefaultStepDto(
                metadata = it.mapWith(metadataMapping),
                spec = it.mapWith(specMapping),
                status = it.mapWith(statusMapping)
            )
        }
    }

    @Bean
    open fun stepMetadataMapping(
        annotationMapper: ToDtoConverter<Any, Any>
    ): Mapping<Step, StepMetadataDto> {
        return Mapping {
            DefaultStepMetadataDto(
                workflowIdentifier = it.workflow.identifier.value,
                stepIdentifier = it.identifier.value,
                kind = it.definition.kind.value,
                annotations = it.metadata.mapValues { metadataEntry ->
                    annotationMapper.toDto(metadataEntry.value)
                }
            )
        }
    }

    @Bean
    open fun stepSpecMapping(
        mapper: ToDtoConverter<Any, Any>
    ): Mapping<Step, StepSpecDto> {
        return Mapping { step ->
            when (step) {
                is StatefulStep -> DefaultStatefulStepSpecDto(
                    data = step.data.associate { dataEntry ->
                        dataEntry.definition.kind.value to dataEntry.get()
                            ?.let { dataValue ->
                                mapper.toDto(dataValue)
                            }
                    }
                )
                else -> DefaultStepSpecDto()
            }
        }
    }

    @Bean
    open fun stepStatusMapping(): Mapping<Step, StepStatusDto> {
        return Mapping { step ->
            DefaultStepStatusDto(
                step.status
            )
        }
    }
}