package de.lise.fluxflow.springboot.rest.workflow.step.data

import de.lise.fluxflow.api.ExperimentalApi
import de.lise.fluxflow.api.step.stateful.data.Data
import de.lise.fluxflow.rest.mapping.Mapping
import de.lise.fluxflow.rest.mapping.ToDtoConverter
import de.lise.fluxflow.rest.workflow.step.data.StepDataDto
import de.lise.fluxflow.rest.workflow.step.data.StepDataMetadataDto
import de.lise.fluxflow.rest.workflow.step.data.StepDataSpecDto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ExperimentalApi
@Configuration
open class StepDataMappingConfiguration {
    @Bean
    open fun stepDataMapping(
        metadataMapping: Mapping<Data<*>, StepDataMetadataDto>,
        specMapping: Mapping<Data<*>, StepDataSpecDto>
    ): Mapping<Data<*>, StepDataDto> {
        return Mapping {
            DefaultStepDataDto(
                metadata = metadataMapping.map(it),
                spec = specMapping.map(it)
            )
        }
    }

    @Bean
    open fun stepDataMetadataMapping(
        mapping: ToDtoConverter<Any, Any>
    ) : Mapping<Data<*>, StepDataMetadataDto> {
        return Mapping {
            DefaultStepDataMetadataDto(
                workflowIdentifier = it.step.workflow.identifier.value,
                stepIdentifier = it.step.identifier.value,
                kind = it.definition.kind.value,
                readonly = it.definition.isReadonly,
                annotations = it.definition.metadata.mapValues { entry ->
                    mapping.toDto(entry.value)
                }
            )
        }
    }

    @Bean
    open fun stepDataSpecMapping(
        mapping: ToDtoConverter<Any, Any>
    ): Mapping<Data<*>, StepDataSpecDto> {
        return Mapping { data ->
            DefaultStepDataSpecDto(
                data.get()?.let { dataValue ->
                    mapping.toDto(dataValue)
                }
            )
        }
    }
}