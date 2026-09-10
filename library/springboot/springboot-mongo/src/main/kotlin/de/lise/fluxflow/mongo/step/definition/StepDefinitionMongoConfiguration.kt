package de.lise.fluxflow.mongo.step.definition

import de.lise.fluxflow.mongo.ConditionalOnFluxFlowMongo
import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.persistence.step.definition.StepDefinitionPersistence
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnFluxFlowMongo
open class StepDefinitionMongoConfiguration {
    @Bean
    open fun stepDefinitionPersistence(
        stepDefinitionRepository: StepDefinitionRepository,
        access: FluxFlowMongoAccess,
    ): StepDefinitionPersistence {
        return StepDefinitionMongoPersistence(
            stepDefinitionRepository,
            access.template,
            access.valueTypes,
        )
    }
}
