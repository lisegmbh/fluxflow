package de.lise.fluxflow.mongo.security.prototype

import de.lise.fluxflow.mongo.flowquery.expression.compilation.MongoCompiler
import de.lise.fluxflow.mongo.flowquery.expression.compilation.SubclassProviderImpl
import de.lise.fluxflow.mongo.flowquery.repository.MongoQueryTranslator
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.mongo.workflow.WorkflowMongoConfiguration
import de.lise.fluxflow.mongo.workflow.WorkflowRepository
import de.lise.fluxflow.mongo.workflow.query.QueryableWorkflowRepositoryImpl
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import org.bson.Document
import org.bson.conversions.Bson
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.MongoConverter
import org.springframework.data.mongodb.core.convert.MongoTypeMapper
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory
import org.springframework.data.projection.EntityProjection
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments

/** Creates the Spring-Data-version-specific, fail-closed type mapper used by the prototype. */
fun interface PrototypeMongoTypeMapperFactory {
    fun create(
        registry: TypeRegistry,
        typeKey: String,
        trustedRootTypes: Set<Class<*>>,
    ): MongoTypeMapper
}

/**
 * Applies the role-specific manifest policy to the raw document before conversion and to the
 * generated document after conversion. The policy deliberately validates without rewriting BSON.
 */
class MongoDocumentTypePolicy(
    private val registry: TypeRegistry,
    private val typeKey: String = "_class",
) {
    fun validate(targetType: Class<*>, source: Bson) {
        if (targetType != WorkflowDocument::class.java) {
            return
        }

        val workflow = source as? Document
            ?: throw IllegalArgumentException("A workflow must be represented by a BSON Document")
        val model = workflow[WorkflowDocument::model.name] ?: return
        if (model is Map<*, *>) {
            if (model.containsKey(typeKey)) {
                resolve(model, TypeRole.MODEL, WorkflowDocument::model.name)
            }
            model.entries
                .asSequence()
                .filterNot { (key, _) -> key == typeKey }
                .forEach { (key, value) -> validateNested(value, "model.$key") }
        } else {
            validateNested(model, WorkflowDocument::model.name)
        }
    }

    private fun validateNested(value: Any?, path: String) {
        when (value) {
            is Map<*, *> -> {
                if (value.containsKey(typeKey)) {
                    resolve(value, TypeRole.VALUE, path)
                }
                value.entries
                    .asSequence()
                    .filterNot { (key, _) -> key == typeKey }
                    .forEach { (key, nested) -> validateNested(nested, "$path.$key") }
            }

            is Iterable<*> -> value.forEachIndexed { index, nested ->
                validateNested(nested, "$path[$index]")
            }

            is Array<*> -> value.forEachIndexed { index, nested ->
                validateNested(nested, "$path[$index]")
            }
        }
    }

    private fun resolve(document: Map<*, *>, role: TypeRole, path: String): Class<*> {
        if (!document.containsKey(typeKey)) {
            throw IllegalArgumentException("BSON document '$path' must contain '$typeKey'")
        }
        val value = document[typeKey]
        val alias = value as? String
            ?: throw IllegalArgumentException("BSON field '$path.$typeKey' must contain a string")
        return registry.resolve(role, alias).java
    }
}

/** MongoConverter decorator that keeps the security check on the synchronous conversion path. */
class GuardedMongoConverter(
    private val delegate: MongoConverter,
    private val policy: MongoDocumentTypePolicy,
) : MongoConverter by delegate {
    override fun <S : Any> read(type: Class<S>, source: Bson): S {
        policy.validate(type, source)
        return delegate.read(type, source)
    }

    override fun <R : Any> project(projection: EntityProjection<R, *>, source: Bson): R {
        policy.validate(projection.domainType.type, source)
        return delegate.project(projection, source)
    }

    override fun write(source: Any, sink: Bson) {
        delegate.write(source, sink)
        policy.validate(source.javaClass, sink)
    }
}

/**
 * Isolated workflow persistence assembled from the host converter as a prototype. It shares the
 * host database factory so Spring-managed Mongo transactions bind to the same resource.
 */
class PrototypeFluxFlowMongoAccess(
    hostTemplate: MongoTemplate,
    databaseFactory: MongoDatabaseFactory,
    registry: TypeRegistry,
    typeMapperFactory: PrototypeMongoTypeMapperFactory,
    typeKey: String = "_class",
) {
    val converter: MongoConverter
    val template: MongoTemplate
    val workflows: WorkflowPersistence

    init {
        val hostConverter = hostTemplate.converter as? MappingMongoConverter
            ?: throw IllegalArgumentException(
                "Prototype access requires a MappingMongoConverter host prototype"
            )
        val isolatedConverter = hostConverter.with(databaseFactory).apply {
            setTypeMapper(
                typeMapperFactory.create(
                    registry,
                    typeKey,
                    setOf(WorkflowDocument::class.java),
                )
            )
        }
        converter = GuardedMongoConverter(
            isolatedConverter,
            MongoDocumentTypePolicy(registry, typeKey),
        )
        template = MongoTemplate(databaseFactory, converter)

        val repository = MongoRepositoryFactory(template).getRepository(
            WorkflowRepository::class.java,
            RepositoryFragments.just(QueryableWorkflowRepositoryImpl(template)),
        )
        val configuration = WorkflowMongoConfiguration()
        val translator = MongoQueryTranslator(MongoCompiler(SubclassProviderImpl(emptySet())))
        workflows = configuration.workflowPersistence(
            repository,
            configuration.workflowFlowQueryRepository(
                translator,
                configuration.workflowMongoExecutor(template),
            ),
            configuration.workflowDocumentMapper(),
        )
    }
}
