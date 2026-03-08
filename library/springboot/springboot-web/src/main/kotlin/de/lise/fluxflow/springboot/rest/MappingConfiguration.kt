package de.lise.fluxflow.springboot.rest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.lise.fluxflow.api.ExperimentalApi
import de.lise.fluxflow.rest.mapping.FromDtoConverter
import de.lise.fluxflow.rest.mapping.ToDtoConverter
import de.lise.fluxflow.springboot.rest.workflow.WorkflowMappingConfiguration
import de.lise.fluxflow.springboot.rest.workflow.step.StepMappingConfiguration
import de.lise.fluxflow.springboot.rest.workflow.step.data.StepDataMappingConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@ExperimentalApi
@Configuration
@Import(
    WorkflowMappingConfiguration::class,
    StepMappingConfiguration::class,
    StepDataMappingConfiguration::class
)
open class MappingConfiguration {
    @Bean
    open fun nonOptionalToDtoConverter(): ToDtoConverter<Any, Any> {
        return ToDtoConverter {
            it
        }
    }

    @Bean
    open fun defaultJacksonConverter(
        objectMapper: ObjectMapper
    ): FromDtoConverter<JsonNode> {
        return FromDtoConverter { element, type ->
            objectMapper.convertValue(
                element,
                objectMapper.typeFactory.constructType(type)
            )
        }
    }
}