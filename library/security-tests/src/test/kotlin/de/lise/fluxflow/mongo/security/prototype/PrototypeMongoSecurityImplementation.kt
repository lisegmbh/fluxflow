package de.lise.fluxflow.mongo.security.prototype

import de.lise.fluxflow.mongo.flowquery.expression.compilation.MongoCompiler
import de.lise.fluxflow.mongo.flowquery.expression.compilation.SubclassProviderImpl
import de.lise.fluxflow.mongo.flowquery.repository.MongoQueryTranslator
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.mongo.workflow.WorkflowMongoConfiguration
import de.lise.fluxflow.mongo.workflow.WorkflowRepository
import de.lise.fluxflow.mongo.workflow.query.QueryableWorkflowRepositoryImpl
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import de.lise.fluxflow.reflection.types.TypeManifestException
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import org.bson.Document
import org.bson.conversions.Bson
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.MongoConverter
import org.springframework.data.mongodb.core.convert.MongoTypeMapper
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory
import org.springframework.data.projection.EntityProjection
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments
import org.springframework.data.mapping.context.MappingContext

/** Creates the Spring-Data-version-specific, fail-closed type mapper used by the prototype. */
fun interface PrototypeMongoTypeMapperFactory {
    fun create(
        registry: TypeRegistry,
        typeKey: String,
        trustedRootTypes: Set<Class<*>>,
    ): MongoTypeMapper
}

/** Version-neutral alias policy shared by the Spring Data 4.x and 5.x adapters. */
internal class PrototypeMongoAliases(
    registry: TypeRegistry,
    trustedRootTypes: Set<Class<*>>,
) {
    private val aliases: Map<String, Class<*>>
    private val roleAliases: Map<Pair<TypeRole, Class<*>>, List<String>>
    private val unscopedAliases: Map<Class<*>, List<String>>

    init {
        val registrations = registry.entries
            .filter { it.role == TypeRole.MODEL || it.role == TypeRole.VALUE }
            .map { Registration(it.key, it.type.java, it.role) } +
                trustedRootTypes.map { Registration(it.name, it, null) }
        aliases = readAliases(registrations)
        roleAliases = aliasesByRoleAndType(registrations)
        unscopedAliases = aliasesByUnscopedType(registrations)
        validateTypeAliases(registrations)
    }

    fun resolve(value: Any?): Class<*> {
        require(value is String) { "Mongo type alias must be a string" }
        require(value.isNotEmpty()) { "Mongo type alias must not be empty" }
        return aliases[value]
            ?: throw IllegalArgumentException("Unregistered Mongo type alias '$value'")
    }

    fun aliasFor(type: Class<*>): String =
        canonicalAlias(TypeRole.MODEL, type)
            ?: canonicalAlias(TypeRole.VALUE, type)
            ?: canonicalUnscopedAlias(type)

    fun aliasFor(role: TypeRole, type: Class<*>): String =
        canonicalAlias(role, type)
            ?: throw IllegalArgumentException(
                "Unregistered Mongo ${role.name.lowercase()} type '${type.name}' cannot be persisted"
            )

    private data class Registration(
        val alias: String,
        val type: Class<*>,
        val role: TypeRole?,
    )

    private fun canonicalAlias(role: TypeRole, type: Class<*>): String? {
        val aliases = roleAliases[role to type] ?: return null
        return aliases.singleOrNull { it == type.name }
            ?: aliases.singleOrNull()
            ?: throw TypeManifestException(
                "Mongo ${role.name.lowercase()} type '${type.name}' has no unique write alias"
            )
    }

    private fun canonicalUnscopedAlias(type: Class<*>): String {
        val aliases = unscopedAliases[type].orEmpty()
        return aliases.singleOrNull { it == type.name }
            ?: aliases.singleOrNull()
            ?: throw IllegalArgumentException(
                "Unregistered Mongo type '${type.name}' cannot be persisted"
            )
    }

    private companion object {
        fun validateTypeAliases(registrations: List<Registration>) {
            registrations
                .asSequence()
                .filter { it.role != null }
                .distinctBy { it.role to it.type }
                .forEach { registration ->
                    val alias = registration.type.getAnnotation(TypeAlias::class.java)?.value
                        ?: return@forEach
                    if (registrations.none {
                            it.role == registration.role &&
                                it.type == registration.type &&
                                it.alias == alias
                        }
                    ) {
                        throw TypeManifestException(
                            "Mongo @TypeAlias '$alias' for '${registration.type.name}' must be registered " +
                                "identically in the type manifest"
                        )
                    }
                }
        }

        fun readAliases(registrations: List<Registration>): Map<String, Class<*>> =
            registrations.groupBy { it.alias }.mapValues { (alias, matches) ->
                val types = matches.map { it.type }.distinct()
                if (types.size != 1) {
                    throw TypeManifestException(
                        "Mongo type alias '$alias' resolves to multiple classes"
                    )
                }
                types.single()
            }

        fun aliasesByRoleAndType(
            registrations: List<Registration>,
        ): Map<Pair<TypeRole, Class<*>>, List<String>> =
            registrations
                .asSequence()
                .filter { it.role != null }
                .groupBy { requireNotNull(it.role) to it.type }
                .mapValues { (_, matches) -> matches.map(Registration::alias).distinct() }

        fun aliasesByUnscopedType(registrations: List<Registration>): Map<Class<*>, List<String>> =
            registrations
                .asSequence()
                .filter { it.role == null }
                .groupBy(Registration::type)
                .mapValues { (_, matches) -> matches.map(Registration::alias).distinct() }

    }
}

/**
 * Applies the role-specific manifest policy to the raw document before conversion and to the
 * generated document after conversion. The policy deliberately validates without rewriting BSON.
 */
class MongoDocumentTypePolicy(
    private val registry: TypeRegistry,
    private val typeKey: String = "_class",
) {
    private val aliases = PrototypeMongoAliases(registry, setOf(WorkflowDocument::class.java))

    fun validate(targetType: Class<*>, source: Bson) {
        val workflow = source as? Document ?: run {
            if (targetType == WorkflowDocument::class.java) {
                throw IllegalArgumentException("A workflow must be represented by a BSON Document")
            }
            return
        }
        if (targetType != WorkflowDocument::class.java && !workflow.isWorkflowDocument()) {
            return
        }
        val model = workflow[WorkflowDocument::model.name] ?: return
        if (model is Map<*, *>) {
            if (model.containsKey(typeKey)) {
                resolve(model, TypeRole.MODEL) { WorkflowDocument::model.name }
            }
            model.entries
                .asSequence()
                .filterNot { (key, _) -> key == typeKey }
                .forEach { (key, value) -> validateNested(value) { "model.$key" } }
        } else {
            validateNested(model) { WorkflowDocument::model.name }
        }
    }

    fun normalizeWriteAliases(targetType: Class<*>, source: Bson) {
        if (targetType != WorkflowDocument::class.java) {
            return
        }

        val workflow = source as? Document
            ?: throw IllegalArgumentException("A workflow must be represented by a BSON Document")
        val model = workflow[WorkflowDocument::model.name] ?: return
        normalizeModel(model) { WorkflowDocument::model.name }
    }

    private fun normalizeModel(value: Any?, path: () -> String) {
        when (value) {
            is Map<*, *> -> {
                if (value.containsKey(typeKey)) {
                    normalizeAlias(value, TypeRole.MODEL, path)
                }
                value.entries
                    .asSequence()
                    .filterNot { (key, _) -> key == typeKey }
                    .forEach { (key, nested) -> normalizeNested(nested) { "${path()}.$key" } }
            }

            else -> normalizeNested(value, path)
        }
    }

    private fun normalizeNested(value: Any?, path: () -> String) {
        when (value) {
            is Map<*, *> -> {
                if (value.containsKey(typeKey)) {
                    normalizeAlias(value, TypeRole.VALUE, path)
                }
                value.entries
                    .asSequence()
                    .filterNot { (key, _) -> key == typeKey }
                    .forEach { (key, nested) -> normalizeNested(nested) { "${path()}.$key" } }
            }

            is Iterable<*> -> value.forEachIndexed { index, nested ->
                normalizeNested(nested) { "${path()}[$index]" }
            }

            is Array<*> -> value.forEachIndexed { index, nested ->
                normalizeNested(nested) { "${path()}[$index]" }
            }
        }
    }

    private fun normalizeAlias(document: Map<*, *>, role: TypeRole, path: () -> String) {
        val alias = document[typeKey] as? String
            ?: throw IllegalArgumentException("BSON field '${path()}.$typeKey' must contain a string")
        val type = aliases.resolve(alias)
        (document as? MutableMap<Any?, Any?>)
            ?.set(typeKey, aliases.aliasFor(role, type))
            ?: throw IllegalArgumentException("BSON document '${path()}' must be mutable while writing")
    }

    private fun validateNested(value: Any?, path: () -> String) {
        when (value) {
            is Map<*, *> -> {
                if (value.containsKey(typeKey)) {
                    resolve(value, TypeRole.VALUE, path)
                }
                value.entries
                    .asSequence()
                    .filterNot { (key, _) -> key == typeKey }
                    .forEach { (key, nested) -> validateNested(nested) { "${path()}.$key" } }
            }

            is Iterable<*> -> value.forEachIndexed { index, nested ->
                validateNested(nested) { "${path()}[$index]" }
            }

            is Array<*> -> value.forEachIndexed { index, nested ->
                validateNested(nested) { "${path()}[$index]" }
            }
        }
    }

    private fun resolve(document: Map<*, *>, role: TypeRole, path: () -> String): Class<*> {
        if (!document.containsKey(typeKey)) {
            throw IllegalArgumentException("BSON document '${path()}' must contain '$typeKey'")
        }
        val value = document[typeKey]
        val alias = value as? String
            ?: throw IllegalArgumentException("BSON field '${path()}.$typeKey' must contain a string")
        return registry.resolve(role, alias).java
    }

    private fun Document.isWorkflowDocument(): Boolean =
        containsKey(WorkflowDocument::model.name)
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
        policy.normalizeWriteAliases(source.javaClass, sink)
    }
}

/**
 * Prevents a new MongoTemplate from installing an auto-index event publisher on the shared host
 * mapping context. All mapping operations still delegate to that context.
 */
private class NonIndexingMongoConverter(
    delegate: MongoConverter,
) : MongoConverter by delegate {
    private val mappingContext = NonIndexingMappingContext(delegate.mappingContext)

    override fun getMappingContext():
        MappingContext<out MongoPersistentEntity<*>, MongoPersistentProperty> = mappingContext
}

private class NonIndexingMappingContext(
    delegate: MappingContext<out MongoPersistentEntity<*>, MongoPersistentProperty>,
) : MappingContext<MongoPersistentEntity<*>, MongoPersistentProperty> by delegate.asMongoMappingContext()

@Suppress("UNCHECKED_CAST")
private fun MappingContext<out MongoPersistentEntity<*>, MongoPersistentProperty>.asMongoMappingContext():
    MappingContext<MongoPersistentEntity<*>, MongoPersistentProperty> =
    this as MappingContext<MongoPersistentEntity<*>, MongoPersistentProperty>

/**
 * Isolated workflow persistence assembled from the host converter as a prototype. It shares the
 * host database factory so Spring-managed Mongo transactions bind to the same resource.
 */
class PrototypeFluxFlowMongoAccess(
    hostTemplate: MongoTemplate,
    databaseFactory: MongoDatabaseFactory,
    registry: TypeRegistry,
    typeMapperFactory: PrototypeMongoTypeMapperFactory,
    typeKey: String? = null,
) {
    val converter: MongoConverter
    val template: MongoTemplate
    val workflows: WorkflowPersistence

    init {
        val hostConverter = hostTemplate.converter as? MappingMongoConverter
            ?: throw IllegalArgumentException(
                "Prototype access requires a MappingMongoConverter host prototype"
            )
        val resolvedHostTypeKey = hostTypeKey(hostConverter)
        val effectiveTypeKey = typeKey ?: resolvedHostTypeKey
        val isolatedConverter = hostConverter.with(databaseFactory).apply {
            setTypeMapper(
                typeMapperFactory.create(
                    registry,
                    effectiveTypeKey,
                    setOf(WorkflowDocument::class.java),
                )
            )
        }
        converter = GuardedMongoConverter(
            isolatedConverter,
            MongoDocumentTypePolicy(registry, effectiveTypeKey),
        )
        template = MongoTemplate(databaseFactory, NonIndexingMongoConverter(converter))
        if (hostTemplate.hasReadPreference()) {
            template.setReadPreference(hostTemplate.readPreference)
        }

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

    private fun hostTypeKey(hostConverter: MappingMongoConverter): String {
        val probe = Document()
        hostConverter.typeMapper.writeType(WorkflowDocument::class.java, probe)
        return probe.keys.singleOrNull() ?: throw IllegalArgumentException(
            "Prototype access requires a host Mongo converter with type metadata enabled"
        )
    }
}
