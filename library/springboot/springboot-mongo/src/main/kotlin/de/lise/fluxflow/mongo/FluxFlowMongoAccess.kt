package de.lise.fluxflow.mongo

import de.lise.fluxflow.mongo.continuation.history.ContinuationRecordDocument
import de.lise.fluxflow.mongo.generic.CollectionType
import de.lise.fluxflow.mongo.generic.NullType
import de.lise.fluxflow.mongo.generic.SimpleType
import de.lise.fluxflow.mongo.generic.ValueTypeConverter
import de.lise.fluxflow.mongo.generic.record.CollectionTypeRecord
import de.lise.fluxflow.mongo.generic.record.JvmTypeRecord
import de.lise.fluxflow.mongo.generic.record.JvmTypeMapping
import de.lise.fluxflow.mongo.generic.record.TypeName
import de.lise.fluxflow.mongo.generic.record.TypeRecordEntry
import de.lise.fluxflow.mongo.generic.record.TypeReference
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.generic.record.NullTypeRecord
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.migration.MigrationDocument
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.definition.StepDefinitionDocument
import de.lise.fluxflow.mongo.step.definition.DataDefinitionDocument
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.reflection.types.TypeRegistry
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.MongoConverter
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments

/**
 * Internal Mongo access used by every FluxFlow repository and direct Mongo operation.
 *
 * The contained template is intentionally not a Spring bean. This keeps Boot's host Mongo bean
 * graph unchanged while all FluxFlow reads share the guarded converter.
 */
class FluxFlowMongoAccess internal constructor(
    hostTemplate: MongoTemplate,
    registry: TypeRegistry,
    typeMapperFactory: FluxFlowMongoTypeMapperFactory,
    applicationContext: ApplicationContext,
    beanFactory: BeanFactory,
    environment: Environment,
    customizers: ObjectProvider<FluxFlowMongoTemplateCustomizer>,
) {
    internal val converter: MongoConverter
    internal val template: MongoTemplate
    internal val typeKey: String
    internal val valueTypes = ValueTypeConverter(registry)
    internal val typeRegistry: TypeRegistry = registry
    internal val typeAliases: FluxFlowMongoTypeAliases
    internal val documentTypePolicy: MongoDocumentTypePolicy

    private val repositoryFactory: MongoRepositoryFactory

    init {
        val hostConverter = hostTemplate.converter as? MappingMongoConverter
            ?: throw IllegalArgumentException(
                "FluxFlow Mongo persistence requires the host MongoTemplate to use a MappingMongoConverter"
            )
        val rootTypes = setOf(
            WorkflowDocument::class.java,
            StepDocument::class.java,
            JobDocument::class.java,
            ContinuationRecordDocument::class.java,
            StepDefinitionDocument::class.java,
            MigrationDocument::class.java,
        )
        val infrastructureTypes = setOf(
            SimpleType::class.java,
            CollectionType::class.java,
            NullType::class.java,
            JvmTypeRecord::class.java,
            CollectionTypeRecord::class.java,
            NullTypeRecord::class.java,
            TypedRecords::class.java,
            JvmTypeMapping::class.java,
            TypeRecordEntry::class.java,
            TypeReference::class.java,
            TypeName::class.java,
            DataDefinitionDocument::class.java,
        )
        typeKey = MongoTypeKeyResolver.resolve(hostConverter.typeMapper)
        typeAliases = FluxFlowMongoTypeAliases(registry, rootTypes, infrastructureTypes)
        val databaseFactory = hostTemplate.mongoDatabaseFactory
        val isolatedConverter = hostConverter.with(databaseFactory).apply {
            setTypeMapper(typeMapperFactory.create(typeAliases, typeKey))
        }
        documentTypePolicy = MongoDocumentTypePolicy(typeAliases, typeKey)
        converter = GuardedMongoConverter(isolatedConverter, documentTypePolicy)
        template = MongoTemplate(databaseFactory, converter).apply {
            setApplicationContext(applicationContext)
            if (hostTemplate.hasReadPreference()) {
                setReadPreference(hostTemplate.readPreference)
            }
            customizers.orderedStream().forEach { it.customize(this) }
        }
        repositoryFactory = MongoRepositoryFactory(template).apply {
            setBeanClassLoader(applicationContext.classLoader ?: FluxFlowMongoAccess::class.java.classLoader)
            setBeanFactory(beanFactory)
            setEnvironment(environment)
        }
    }

    internal fun <T : Any> repository(type: Class<T>): T = repositoryFactory.getRepository(type)

    internal fun <T : Any> repository(type: Class<T>, fragment: Any): T =
        repositoryFactory.getRepository(type, RepositoryFragments.just(fragment))
}
