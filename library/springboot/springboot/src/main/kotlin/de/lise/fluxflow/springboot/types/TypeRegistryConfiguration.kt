package de.lise.fluxflow.springboot.types

import de.lise.fluxflow.reflection.types.TypeRegistry
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
open class TypeRegistryConfiguration {
    @Bean
    @ConditionalOnMissingBean(TypeRegistry::class)
    open fun fluxFlowTypeRegistry(
        context: ApplicationContext,
        contributors: ObjectProvider<TypeRegistrationContributor>,
    ): TypeRegistry = FluxFlowTypeRegistryFactory(
        context,
        context.classLoader ?: TypeRegistryConfiguration::class.java.classLoader,
        contributors.orderedStream().toList(),
    ).create()
}
