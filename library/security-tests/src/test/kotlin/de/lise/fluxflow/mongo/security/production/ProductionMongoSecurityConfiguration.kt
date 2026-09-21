package de.lise.fluxflow.mongo.security.production

import com.mongodb.ReadPreference
import de.lise.fluxflow.mongo.FluxFlowMongoTemplateCustomizer
import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader
import de.lise.fluxflow.mongo.security.fixtures.SecurityConvertedValueReader
import de.lise.fluxflow.mongo.security.fixtures.SecurityConvertedValueWriter
import de.lise.fluxflow.mongo.security.fixtures.SecurityHostRepository
import de.lise.fluxflow.mongo.security.fixtures.allowedSecurityTestEntries
import de.lise.fluxflow.mongo.security.fixtures.securityTestRegistry
import de.lise.fluxflow.reflection.types.TypeRegistry
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.core.annotation.Order
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import java.util.concurrent.CopyOnWriteArrayList

@TestConfiguration
@EnableMongoRepositories(basePackageClasses = [SecurityHostRepository::class])
open class ProductionMongoSecurityConfiguration {
    @Bean
    open fun productionMongoCustomizerInvocations(): MongoCustomizerInvocations =
        MongoCustomizerInvocations()

    @Bean
    @Order(10)
    open fun firstProductionMongoTemplateCustomizer(
        invocations: MongoCustomizerInvocations,
    ): FluxFlowMongoTemplateCustomizer = FluxFlowMongoTemplateCustomizer {
        invocations.values += it to "first"
        it.setReadPreference(ReadPreference.primary())
    }

    @Bean
    @Order(20)
    open fun secondProductionMongoTemplateCustomizer(
        invocations: MongoCustomizerInvocations,
    ): FluxFlowMongoTemplateCustomizer = FluxFlowMongoTemplateCustomizer {
        invocations.values += it to "second"
        it.setReadPreference(ReadPreference.nearest())
    }

    @Bean("productionWitnessClassLoader")
    open fun productionWitnessClassLoader(): WitnessClassLoader = WitnessClassLoader()

    @Bean
    @Primary
    open fun productionTypeRegistry(
        @org.springframework.beans.factory.annotation.Qualifier("productionWitnessClassLoader")
        loader: WitnessClassLoader,
    ): TypeRegistry = securityTestRegistry(loader, *allowedSecurityTestEntries())

    @Bean
    open fun mongoCustomConversions(): MongoCustomConversions =
        MongoCustomConversions.create { adapter ->
            adapter.registerConverter(SecurityConvertedValueWriter)
            adapter.registerConverter(SecurityConvertedValueReader)
        }

    @Bean
    open fun witnessAwareHostConverter(
        @org.springframework.beans.factory.annotation.Qualifier("productionWitnessClassLoader")
        loader: WitnessClassLoader,
        @Value("\${fluxflow.security.test.type-key:_class}")
        typeKey: String,
    ): BeanPostProcessor = object : BeanPostProcessor {
        override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
            if (bean is MappingMongoConverter) {
                bean.setTypeMapper(
                    DefaultMongoTypeMapper(typeKey, bean.mappingContext).apply {
                        setBeanClassLoader(loader)
                    }
                )
            }
            if (bean is MongoTemplate) {
                bean.setReadPreference(ReadPreference.secondaryPreferred())
            }
            return bean
        }
    }
}

class MongoCustomizerInvocations {
    val values = CopyOnWriteArrayList<Pair<MongoTemplate, String>>()
}
