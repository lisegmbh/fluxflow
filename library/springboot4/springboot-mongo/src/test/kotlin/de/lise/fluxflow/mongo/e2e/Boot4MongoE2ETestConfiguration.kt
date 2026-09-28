package de.lise.fluxflow.mongo.e2e

import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.TypeRegistration
import de.lise.fluxflow.mongo.MongoConfiguration
import de.lise.fluxflow.springboot.configuration.BasicConfiguration
import de.lise.fluxflow.springboot.types.TypeRegistrationContributor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(BasicConfiguration::class, MongoConfiguration::class)
@ComponentScan(basePackageClasses = [Boot4MongoE2ETestConfiguration::class])
open class Boot4MongoE2ETestConfiguration {
    @Bean
    open fun boot4MongoE2EModelTypes(): TypeRegistrationContributor =
        TypeRegistrationContributor {
            listOf(
                TypeRegistration(
                    TypeRole.MODEL,
                    Boot4MongoE2EWorkflowModel::class.java.name,
                    Boot4MongoE2EWorkflowModel::class,
                ),
                TypeRegistration(
                    TypeRole.VALUE,
                    Boot4MongoE2EWorkflowModel::class.java.name,
                    Boot4MongoE2EWorkflowModel::class,
                ),
            )
        }
}
