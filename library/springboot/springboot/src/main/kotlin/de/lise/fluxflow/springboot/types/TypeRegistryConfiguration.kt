package de.lise.fluxflow.springboot.types

import de.lise.fluxflow.reflection.types.TypeRegistry
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration(proxyBeanMethods = false)
open class TypeRegistryConfiguration {
    @Bean
    @Primary
    open fun fluxFlowTypeRegistry(
        context: ApplicationContext,
        contributors: Map<String, TypeRegistrationContributor>,
    ): TypeRegistry = FluxFlowTypeRegistryFactory(
        context,
        context.classLoader ?: TypeRegistryConfiguration::class.java.classLoader,
        contributors,
    ).create()
}
