package de.lise.fluxflow.mongo.security.prototype

import com.mongodb.ReadPreference
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.MongoConverter

/** A host template fixture whose read preference must be preserved by the isolated access. */
@TestConfiguration
open class PrototypeMongoHostTemplateFixture {
    @Bean
    @Primary
    open fun mongoTemplate(
        databaseFactory: MongoDatabaseFactory,
        converter: MongoConverter,
    ): MongoTemplate = MongoTemplate(databaseFactory, converter).apply {
        setReadPreference(ReadPreference.secondaryPreferred())
    }
}
