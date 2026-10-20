package de.lise.fluxflow.springboot.autoconfigure

import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import de.lise.fluxflow.springboot.web.SpringWebParameterResolver
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper

@Configuration
open class SpringWebSupportConfiguration {
    @Bean
    open fun springWebParameterResolver(
        configurableBeanFactory: ConfigurableBeanFactory,
        objectMapper: ObjectMapper
    ): ParameterResolver {
        return SpringWebParameterResolver(
            configurableBeanFactory.typeConverter,
            objectMapper
        )
    }
}
