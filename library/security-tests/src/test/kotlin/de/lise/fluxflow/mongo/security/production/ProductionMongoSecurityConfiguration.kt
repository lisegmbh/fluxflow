package de.lise.fluxflow.mongo.security.production

import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader
import de.lise.fluxflow.mongo.security.prototype.PrototypeConvertedValueReader
import de.lise.fluxflow.mongo.security.prototype.PrototypeConvertedValueWriter
import de.lise.fluxflow.mongo.security.prototype.allowedPrototypeEntries
import de.lise.fluxflow.mongo.security.prototype.prototypeRegistry
import de.lise.fluxflow.reflection.types.TypeRegistry
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions

@TestConfiguration
open class ProductionMongoSecurityConfiguration {
    @Bean("productionWitnessClassLoader")
    open fun productionWitnessClassLoader(): WitnessClassLoader = WitnessClassLoader()

    @Bean
    @Primary
    open fun productionTypeRegistry(
        @org.springframework.beans.factory.annotation.Qualifier("productionWitnessClassLoader")
        loader: WitnessClassLoader,
    ): TypeRegistry = prototypeRegistry(loader, *allowedPrototypeEntries())

    @Bean
    open fun mongoCustomConversions(): MongoCustomConversions =
        MongoCustomConversions.create { adapter ->
            adapter.registerConverter(PrototypeConvertedValueWriter)
            adapter.registerConverter(PrototypeConvertedValueReader)
        }

    @Bean
    open fun witnessAwareHostConverter(
        @org.springframework.beans.factory.annotation.Qualifier("productionWitnessClassLoader")
        loader: WitnessClassLoader,
    ): BeanPostProcessor = object : BeanPostProcessor {
        override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
            if (bean is MappingMongoConverter) {
                bean.setTypeMapper(
                    DefaultMongoTypeMapper("_class", bean.mappingContext).apply {
                        setBeanClassLoader(loader)
                    }
                )
            }
            return bean
        }
    }
}
