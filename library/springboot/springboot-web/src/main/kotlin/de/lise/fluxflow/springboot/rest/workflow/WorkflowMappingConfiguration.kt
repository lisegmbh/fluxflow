@file:OptIn(ExperimentalApi::class)

package de.lise.fluxflow.springboot.rest.workflow

import de.lise.fluxflow.api.ExperimentalApi
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.rest.mapping.Mapping
import de.lise.fluxflow.rest.mapping.ToDtoConverter
import de.lise.fluxflow.rest.workflow.WorkflowDto
import de.lise.fluxflow.rest.workflow.WorkflowMetadataDto
import de.lise.fluxflow.rest.workflow.WorkflowSpecDto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class WorkflowMappingConfiguration {
    @Bean
    open fun workflowMapping(
        metadataMapping: Mapping<Workflow<*>, WorkflowMetadataDto>,
        specMapping: Mapping<Workflow<*>, WorkflowSpecDto>
    ): Mapping<Workflow<*>, WorkflowDto> {
        return Mapping {
            DefaultWorkflowDto(
                metadata = metadataMapping.map(it),
                spec = specMapping.map(it)
            )
        }
    }

    @Bean
    open fun workflowMetadataMapping(
        mapper: ToDtoConverter<Any, Any>
    ): Mapping<Workflow<*>, WorkflowMetadataDto> {
        return Mapping {
            DefaultWorkflowMetadataDto(
                it.identifier.value,
                annotations = it.metadata.mapValues { entry ->
                    mapper.toDto(entry.value)
                }
            )
        }
    }

    @Bean
    open fun workflowSpecMapping(
        mapper: ToDtoConverter<Any, Any>
    ): Mapping<Workflow<*>, WorkflowSpecDto> {
        return Mapping {
            DefaultWorkflowSpecDto(
                model = it.model?.let { model ->
                    mapper.toDto(model)
                }
            )
        }
    }
}