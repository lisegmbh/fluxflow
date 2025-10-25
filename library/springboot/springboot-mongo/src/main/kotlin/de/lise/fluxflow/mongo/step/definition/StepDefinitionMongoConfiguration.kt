package de.lise.fluxflow.mongo.step.definition

import de.lise.fluxflow.mongo.ConditionalOnFluxFlowMongo
import de.lise.fluxflow.persistence.step.definition.StepDefinitionPersistence
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate

@Configuration
@ConditionalOnFluxFlowMongo
open class StepDefinitionMongoConfiguration {
    @Bean
    open fun stepDefinitionPersistence(
        stepDefinitionRepository: StepDefinitionRepository,
        mongoTemplate: MongoTemplate
    ): StepDefinitionPersistence {
        return StepDefinitionMongoPersistence(stepDefinitionRepository, mongoTemplate)
    }
}