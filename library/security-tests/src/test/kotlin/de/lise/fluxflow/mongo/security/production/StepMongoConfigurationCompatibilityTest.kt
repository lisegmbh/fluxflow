package de.lise.fluxflow.mongo.security.production

import de.fluxflow.flowquery.mapper.query.QueryMapper
import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.mongo.flowquery.repository.MongoFlowQueryRepository
import de.lise.fluxflow.mongo.flowquery.repository.MongoQueryTranslator
import de.lise.fluxflow.mongo.generic.ValueTypeConverter
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.StepMongoConfiguration
import de.lise.fluxflow.mongo.step.StepRepository
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.StepPersistence
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate

class StepMongoConfigurationCompatibilityTest {
    @Test
    fun `legacy three argument override supplies the Spring persistence bean`() {
        val access = Mockito.mock(FluxFlowMongoAccess::class.java)
        Mockito.doReturn(Mockito.mock(MongoTemplate::class.java)).`when`(access).template
        Mockito.doReturn(ValueTypeConverter.builtInsOnly()).`when`(access).valueTypes

        AnnotationConfigApplicationContext().use { context ->
            context.beanFactory.registerSingleton("mongoAccess", access)
            context.beanFactory.registerSingleton("stepRepository", Mockito.mock(StepRepository::class.java))
            context.beanFactory.registerSingleton("queryTranslator", Mockito.mock(MongoQueryTranslator::class.java))
            context.register(LegacyStepMongoConfiguration::class.java)
            context.refresh()

            assertThat(context.getBean(StepPersistence::class.java))
                .isSameAs(context.getBean(LegacyStepMongoConfiguration::class.java).customPersistence)
        }
    }

    @Configuration(proxyBeanMethods = false)
    open class LegacyStepMongoConfiguration : StepMongoConfiguration() {
        val customPersistence: StepPersistence = Mockito.mock(StepPersistence::class.java)

        @Bean
        override fun stepPersistence(
            stepRepository: StepRepository,
            queryableRepository: MongoFlowQueryRepository<StepDocument>,
            queryMapper: QueryMapper<StepData, StepDocument>,
        ): StepPersistence = customPersistence
    }
}
