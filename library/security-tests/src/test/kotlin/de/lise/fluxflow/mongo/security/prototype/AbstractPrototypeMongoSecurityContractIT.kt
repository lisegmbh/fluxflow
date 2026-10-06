package de.lise.fluxflow.mongo.security.prototype

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.set
import de.fluxflow.flowquery.query.FlowQuery
import de.fluxflow.flowquery.expression.ExpressionExtensions.Types.isType
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.mongo.workflow.WorkflowRepository
import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader
import de.lise.fluxflow.persistence.workflow.WorkflowData
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.TypeManifestException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bson.Document
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.MongoTransactionManager
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.GenericContainer
import java.util.UUID

abstract class AbstractPrototypeMongoSecurityContractIT {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    protected lateinit var hostTemplate: MongoTemplate

    @Autowired
    private lateinit var databaseFactory: MongoDatabaseFactory

    @Autowired
    private lateinit var hostWorkflowRepository: WorkflowRepository

    @Autowired
    private lateinit var hostWorkflowPersistence: WorkflowPersistence

    @Autowired
    private lateinit var hostRepository: PrototypeHostRepository

    @Autowired
    private lateinit var contextPrototypeAccess: PrototypeFluxFlowMongoAccess

    @Autowired
    private lateinit var typeMapperFactory: PrototypeMongoTypeMapperFactory

    @BeforeEach
    fun clearWorkflows() {
        if (!hostTemplate.collectionExists(WorkflowDocument::class.java)) {
            hostTemplate.createCollection(WorkflowDocument::class.java)
        }
        hostTemplate.remove(Query(), WorkflowDocument::class.java)
    }

    @Test
    fun `D00 security fixture pins the Mongo image`() {
        val containers = applicationContext.getBeansOfType(GenericContainer::class.java).values

        assertThat(containers)
            .describedAs("The security suite must use exactly one explicitly pinned Mongo fixture")
            .hasSize(1)
        assertThat(containers.single().dockerImageName).isEqualTo("mongo:8.0.12")
    }

    @Test
    fun `D07 direct converter rejects malformed and untrusted model type metadata`() {
        val loader = WitnessClassLoader()
        val registry = prototypeRegistry(
            loader,
            prototypeEntry(TypeRole.VALUE, PROTOTYPE_WITNESS_NAME, PROTOTYPE_WITNESS_NAME),
        )
        val converter = prototypeAccess(registry).converter
        assertThat(loader.events).isEmpty()

        val wrongRole = catchFailure {
            converter.read(
                WorkflowDocument::class.java,
                rawWorkflow("wrong-role", "_class", PROTOTYPE_WITNESS_NAME),
            )
        }
        assertUnknownType(wrongRole, TypeRole.MODEL, PROTOTYPE_WITNESS_NAME)

        val unknown = catchFailure {
            converter.read(
                WorkflowDocument::class.java,
                rawWorkflow("unknown", "_class", "not.registered.Model"),
            )
        }
        assertUnknownType(unknown, TypeRole.MODEL, "not.registered.Model")

        val empty = catchFailure {
            converter.read(
                WorkflowDocument::class.java,
                rawWorkflow("empty", "_class", ""),
            )
        }
        assertUnknownType(empty, TypeRole.MODEL, "")

        assertThatThrownBy {
            converter.read(
                WorkflowDocument::class.java,
                rawWorkflow("non-string", "_class", 42),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("_class")

        val unknownRootAlias = "not.registered.Root"
        val unknownRootFailure = catchFailure {
            converter.read(
                WorkflowDocument::class.java,
                Document("_id", "unknown-root")
                    .append("model", "safe-scalar")
                    .append("modelType", String::class.java.name)
                    .append("_class", unknownRootAlias),
            )
        }
        assertThat(
            generateSequence(unknownRootFailure as Throwable?) { it.cause }
                .mapNotNull { it.message }
                .toList()
        ).anySatisfy { message ->
            assertThat(message)
                .contains("Unregistered Mongo type alias")
                .contains(unknownRootAlias)
        }

        assertThat(loader.events)
            .describedAs("Rejected metadata must not initialize or construct its referenced class")
            .isEmpty()
    }

    @Test
    fun `D07 direct converter guards workflow metadata even when the requested result is Any`() {
        val loader = WitnessClassLoader()
        val alias = PROTOTYPE_WITNESS_NAME
        val registry = prototypeRegistry(
            loader,
            prototypeEntry(TypeRole.VALUE, alias, PROTOTYPE_WITNESS_NAME),
        )
        val converter = prototypeAccess(registry).converter

        val failure = catchFailure {
            converter.read(
                Any::class.java,
                rawWorkflow("any-result-type", "_class", alias),
            )
        }

        assertUnknownType(failure, TypeRole.MODEL, alias)
        assertThat(loader.events)
            .describedAs("The result type must not let untrusted workflow BSON reach materialization")
            .isEmpty()
    }

    @Test
    fun `D07 Any result guards workflow BSON with no modelType before materialization`() {
        val loader = WitnessClassLoader()
        val alias = PROTOTYPE_WITNESS_NAME
        val registry = prototypeRegistry(
            loader,
            prototypeEntry(TypeRole.VALUE, alias, PROTOTYPE_WITNESS_NAME),
        )
        val converter = prototypeAccess(registry).converter
        val rawWorkflowWithoutModelType = Document("_id", "any-result-without-model-type")
            .append("model", Document("_class", alias))

        val failure = catchFailure {
            converter.read(Any::class.java, rawWorkflowWithoutModelType)
        }

        assertUnknownType(failure, TypeRole.MODEL, alias)
        assertThat(loader.events)
            .describedAs("A missing modelType must not let wrong-role BSON reach materialization")
            .isEmpty()
    }

    @Test
    fun `D07 custom type keys stay guarded when the requested result is Any`() {
        val loader = WitnessClassLoader()
        val alias = PROTOTYPE_WITNESS_NAME
        val typeKey = "@type"
        val registry = prototypeRegistry(
            loader,
            prototypeEntry(TypeRole.VALUE, alias, PROTOTYPE_WITNESS_NAME),
        )
        val converter = prototypeAccess(registry, typeKey).converter

        val failure = catchFailure {
            converter.read(
                Any::class.java,
                rawWorkflow("custom-key-any-result-type", typeKey, alias),
            )
        }

        assertUnknownType(failure, TypeRole.MODEL, alias)
        assertThat(loader.events)
            .describedAs("The configured discriminator must be guarded before materialization")
            .isEmpty()
    }

    @Test
    fun `D09 access derives the persisted type key from the host converter`() {
        val customTypeKey = "@hostType"
        val customHostConverter = (hostTemplate.converter as MappingMongoConverter)
            .with(databaseFactory)
            .apply {
                setTypeMapper(DefaultMongoTypeMapper(customTypeKey))
            }
        val registry = prototypeRegistry(WitnessClassLoader(), *allowedPrototypeEntries())
        val access = PrototypeFluxFlowMongoAccess(
            hostTemplate = MongoTemplate(databaseFactory, customHostConverter),
            databaseFactory = databaseFactory,
            registry = registry,
            typeMapperFactory = typeMapperFactory,
        )
        val written = Document()

        access.converter.write(
            WorkflowDocument("host-type-key", prototypeModel(), PrototypeWorkflowModel::class.java.name),
            written,
        )

        val persistedModel = written.get("model", Document::class.java)
        assertThat(persistedModel.getString(customTypeKey))
            .isEqualTo(PrototypeWorkflowModel::class.java.name)
        assertThat(persistedModel.containsKey("_class")).isFalse()
    }

    @Test
    fun `D07 access creation rejects a host converter with type metadata disabled`() {
        val disabledHostConverter = (hostTemplate.converter as MappingMongoConverter)
            .with(databaseFactory)
            .apply {
                setTypeMapper(DefaultMongoTypeMapper(null))
            }
        val disabledHostTemplate = MongoTemplate(databaseFactory, disabledHostConverter)
        val registry = prototypeRegistry(WitnessClassLoader(), *allowedPrototypeEntries())

        assertThatThrownBy {
            PrototypeFluxFlowMongoAccess(
                hostTemplate = disabledHostTemplate,
                databaseFactory = databaseFactory,
                registry = registry,
                typeMapperFactory = typeMapperFactory,
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("type")
    }

    @Test
    fun `D08 direct converter applies VALUE role below collections maps and nulls`() {
        val loader = WitnessClassLoader()
        val registry = prototypeRegistry(
            loader,
            prototypeEntry(
                TypeRole.MODEL,
                PrototypeWorkflowModel::class.java.name,
                PrototypeWorkflowModel::class.java.name,
            ),
            prototypeEntry(TypeRole.MODEL, PROTOTYPE_WITNESS_NAME, PROTOTYPE_WITNESS_NAME),
        )
        val converter = prototypeAccess(registry).converter
        val raw = rawWorkflow(
            id = "nested-value",
            typeKey = "_class",
            modelType = PrototypeWorkflowModel::class.java.name,
            modelFields = mapOf(
                "name" to "nested-value",
                "converted" to "converted:nested-value",
                "nested" to listOf(
                    linkedMapOf(
                        "null" to null,
                        "payload" to Document("_class", PROTOTYPE_WITNESS_NAME),
                    )
                ),
            ),
        )

        val failure = catchFailure {
            converter.read(WorkflowDocument::class.java, raw)
        }

        assertUnknownType(failure, TypeRole.VALUE, PROTOTYPE_WITNESS_NAME)
        assertThat(loader.events)
            .describedAs("Role rejection must happen before nested value materialization")
            .isEmpty()
    }

    @Test
    fun `D08 ambiguous aliases across Mongo roles fail during prototype construction`() {
        val loader = WitnessClassLoader()
        val ambiguousAlias = "ambiguous"
        val registry = prototypeRegistry(
            loader,
            prototypeEntry(
                TypeRole.MODEL,
                ambiguousAlias,
                PrototypeWorkflowModel::class.java.name,
            ),
            prototypeEntry(
                TypeRole.VALUE,
                ambiguousAlias,
                PrototypeWorkflowValue::class.java.name,
            ),
        )

        assertThatThrownBy { prototypeAccess(registry) }
            .isInstanceOf(TypeManifestException::class.java)
            .hasMessageContaining(ambiguousAlias)
    }

    @Test
    fun `D08 same type may use distinct MODEL and VALUE aliases`() {
        val registry = prototypeRegistry(
            WitnessClassLoader(),
            prototypeEntry(
                TypeRole.MODEL,
                DUAL_ROLE_MODEL_ALIAS,
                PrototypeWorkflowValue::class.java.name,
            ),
            prototypeEntry(
                TypeRole.VALUE,
                DUAL_ROLE_VALUE_ALIAS,
                PrototypeWorkflowValue::class.java.name,
            ),
        )
        val converter = prototypeAccess(registry).converter

        val document = converter.read(
            WorkflowDocument::class.java,
            rawWorkflow(
                id = "dual-role-aliases",
                typeKey = "_class",
                modelType = DUAL_ROLE_MODEL_ALIAS,
                modelFields = mapOf(
                    "value" to "model-value",
                    "nested" to Document("_class", DUAL_ROLE_VALUE_ALIAS)
                        .append("value", "nested-value"),
                ),
            ),
        )

        assertThat(document.model).isEqualTo(PrototypeWorkflowValue("model-value"))
    }

    @Test
    fun `D08 writes the canonical alias for each role of the same type`() {
        val registry = prototypeRegistry(
            WitnessClassLoader(),
            prototypeEntry(
                TypeRole.MODEL,
                DUAL_ROLE_MODEL_ALIAS,
                PrototypeDualRoleValue::class.java.name,
            ),
            prototypeEntry(
                TypeRole.VALUE,
                DUAL_ROLE_VALUE_ALIAS,
                PrototypeDualRoleValue::class.java.name,
            ),
        )
        val document = Document()

        prototypeAccess(registry).converter.write(
            WorkflowDocument(
                "dual-role-write",
                PrototypeDualRoleValue(
                    "model-value",
                    PrototypeDualRoleValue("nested-value"),
                ),
                PrototypeDualRoleValue::class.java.name,
            ),
            document,
        )

        val persistedModel = document.get("model", Document::class.java)
        assertThat(persistedModel.getString("_class")).isEqualTo(DUAL_ROLE_MODEL_ALIAS)
        assertThat(persistedModel.get("nested", Document::class.java).getString("_class"))
            .isEqualTo(DUAL_ROLE_VALUE_ALIAS)
    }

    @Test
    fun `D08 unrelated historical aliases do not prevent prototype construction`() {
        val registry = registryWithAmbiguousValueWriteAliases()

        val access = prototypeAccess(registry)

        assertThat(access.converter).isNotNull()
    }

    @Test
    fun `D08 ambiguous write aliases fail only when the affected type is written`() {
        val access = prototypeAccess(registryWithAmbiguousValueWriteAliases())

        assertThatThrownBy {
            access.converter.write(PrototypeWorkflowValue("ambiguous-write"), Document())
        }
            .isInstanceOf(TypeManifestException::class.java)
            .hasMessageContaining("has no unique write alias")
    }

    @Test
    fun `D08 historical value aliases remain readable but are not emitted on writes`() {
        val registry = prototypeRegistry(
            WitnessClassLoader(),
            prototypeEntry(
                TypeRole.VALUE,
                PrototypeWorkflowValue::class.java.name,
                PrototypeWorkflowValue::class.java.name,
            ),
            prototypeEntry(
                TypeRole.VALUE,
                LEGACY_VALUE_ALIAS,
                PrototypeWorkflowValue::class.java.name,
            ),
            prototypeEntry(
                TypeRole.VALUE,
                RENAMED_VALUE_ALIAS,
                PrototypeWorkflowValue::class.java.name,
            ),
        )
        val converter = prototypeAccess(registry).converter

        listOf(LEGACY_VALUE_ALIAS, RENAMED_VALUE_ALIAS).forEach { alias ->
            assertThat(
                converter.read(
                    PrototypeWorkflowValue::class.java,
                    Document("_class", alias).append("value", alias),
                ),
            ).isEqualTo(PrototypeWorkflowValue(alias))
        }

        val written = Document()
        converter.write(PrototypeWorkflowValue("canonical-write"), written)

        assertThat(written.getString("_class"))
            .isEqualTo(PrototypeWorkflowValue::class.java.name)
            .isNotIn(LEGACY_VALUE_ALIAS, RENAMED_VALUE_ALIAS)
    }

    @Test
    fun `D08 TypeAlias round-trips only when the identical alias is registered in the manifest`() {
        val registry = prototypeRegistry(
            WitnessClassLoader(),
            prototypeEntry(
                TypeRole.MODEL,
                TYPE_ALIAS_MANIFEST_ALIAS,
                TypeAliasedWorkflowModel::class.java.name,
            ),
        )
        val access = prototypeAccess(registry)
        val identifier = WorkflowIdentifier(UUID.randomUUID().toString())
        val model = TypeAliasedWorkflowModel("manifest-controlled")

        access.workflows.create(model, identifier)

        val persisted = requireNotNull(
            workflowCollection(access.template).find(eq("_id", identifier.value)).first(),
        )
        assertThat(persisted.get("model", Document::class.java).getString("_class"))
            .isEqualTo(TYPE_ALIAS_MANIFEST_ALIAS)
        assertThat(access.workflows.find(identifier)?.model).isEqualTo(model)
    }

    @Test
    fun `D08 TypeAlias not registered identically in the manifest rejects prototype construction`() {
        val registry = prototypeRegistry(
            WitnessClassLoader(),
            prototypeEntry(
                TypeRole.MODEL,
                TypeAliasedWorkflowModel::class.java.name,
                UnregisteredTypeAliasedWorkflowModel::class.java.name,
            ),
        )
        assertThatThrownBy {
            prototypeAccess(registry)
        }
            .isInstanceOf(TypeManifestException::class.java)
            .hasMessageContaining("@TypeAlias")
            .hasMessageContaining(UNREGISTERED_TYPE_ALIAS)
    }

    @Test
    fun `D09 workflow reads stay guarded when lifecycle events are disabled`() {
        val loader = WitnessClassLoader()
        val registry = prototypeRegistry(
            loader,
            prototypeEntry(TypeRole.VALUE, PROTOTYPE_WITNESS_NAME, PROTOTYPE_WITNESS_NAME),
        )
        val access = prototypeAccess(registry)

        access.template.setEntityLifecycleEventsEnabled(false)
        val disabledEventsId = UUID.randomUUID().toString()
        insertRaw(
            access.template,
            rawWorkflow(disabledEventsId, "_class", PROTOTYPE_WITNESS_NAME),
        )
        val disabledFailure = catchFailure {
            access.workflows.find(WorkflowIdentifier(disabledEventsId))
        }
        assertUnknownType(disabledFailure, TypeRole.MODEL, PROTOTYPE_WITNESS_NAME)

        assertThat(loader.events)
            .describedAs("Security must not depend on Mongo lifecycle listeners")
            .isEmpty()
    }

    @Test
    fun `D09 internal repository and flow query reject an untrusted workflow model before materialization`() {
        val loader = WitnessClassLoader()
        val alias = PROTOTYPE_WITNESS_NAME
        val registry = prototypeRegistry(
            loader,
            prototypeEntry(TypeRole.VALUE, alias, PROTOTYPE_WITNESS_NAME),
        )
        val access = prototypeAccess(registry)
        val identifier = WorkflowIdentifier(UUID.randomUUID().toString())
        insertRaw(access.template, rawWorkflow(identifier.value, "_class", alias))

        val repositoryFailure = catchFailure {
            access.workflows.find(identifier)
        }
        assertUnknownType(repositoryFailure, TypeRole.MODEL, alias)

        val queryFailure = catchFailure {
            access.workflows.findAll(
                FlowQuery.of {
                    where {
                        get(WorkflowData::id).isEqual(identifier.value)
                    }
                }
            )
        }
        assertUnknownType(queryFailure, TypeRole.MODEL, alias)
        assertThat(loader.events)
            .describedAs("Neither internal read path may materialize the untrusted model")
            .isEmpty()
    }

    @Test
    fun `D08 D09 FQCN aliases custom type keys and supported values round-trip`() {
        val loader = WitnessClassLoader()
        val registry = prototypeRegistry(loader, *allowedPrototypeEntries())
        val typeKey = "@type"
        val access = prototypeAccess(registry, typeKey)
        val identifier = WorkflowIdentifier(UUID.randomUUID().toString())
        val model = prototypeModel()

        access.workflows.create(model, identifier)

        val collection = workflowCollection(access.template)
        val persisted = requireNotNull(collection.find(eq("_id", identifier.value)).first())
        val persistedModel = persisted.get("model", Document::class.java)
        assertThat(persistedModel.getString(typeKey)).isEqualTo(PrototypeWorkflowModel::class.java.name)
        assertThat(persistedModel.containsKey("_class")).isFalse()
        assertThat(persistedModel.getString("mapped_name")).isEqualTo(model.name)
        assertThat(persistedModel.containsKey("name")).isFalse()
        assertThat(persistedModel.getString("converted")).isEqualTo("converted:custom-conversion")

        assertThat(access.workflows.find(identifier)?.model).isEqualTo(model)

        val aliases = com.mongodb.client.model.Updates.combine(
            set("model.$typeKey", MODEL_TYPE_ALIAS),
            set("model.nested.0.alias.$typeKey", VALUE_TYPE_ALIAS),
        )
        val update = collection.updateOne(eq("_id", identifier.value), aliases)
        assertThat(update.matchedCount).isEqualTo(1)
        assertThat(update.modifiedCount).isEqualTo(1)

        val aliased = access.workflows.find(identifier)
        assertThat(aliased?.model).isEqualTo(model)
        assertThat((aliased?.model as PrototypeWorkflowModel).nested.single()["null"]).isNull()

        val subtypeIdentifier = WorkflowIdentifier(UUID.randomUUID().toString())
        val subtype: PrototypeWorkflowModelType = PrototypeWorkflowSubtype(
            name = "allowed-subtype",
            subtypeValue = "preserved",
        )
        access.workflows.create(subtype, subtypeIdentifier)
        assertThat(access.workflows.find(subtypeIdentifier)?.model)
            .isEqualTo(subtype)
            .isInstanceOf(PrototypeWorkflowSubtype::class.java)
    }

    @Test
    fun `D09 null scalar collection and map workflow models round-trip`() {
        val access = contextPrototypeAccess
        val cases = listOf<Any?>(
            null,
            "scalar",
            listOf("one", 2, null),
            linkedMapOf("string" to "value", "null" to null),
        )

        cases.forEach { model ->
            val identifier = WorkflowIdentifier(UUID.randomUUID().toString())
            access.workflows.create(model, identifier)

            assertThat(access.workflows.find(identifier)?.model).isEqualTo(model)
        }
    }

    @Test
    fun `D11 prototype access leaves host beans converter and repository unchanged`() {
        val templateBean = applicationContext.getBean(MongoTemplate::class.java)
        val repositoryBean = applicationContext.getBean(WorkflowRepository::class.java)
        val persistenceBean = applicationContext.getBean(WorkflowPersistence::class.java)
        val hostConverter = hostTemplate.converter

        val access = contextPrototypeAccess

        assertThat(templateBean).isSameAs(hostTemplate)
        assertThat(repositoryBean).isSameAs(hostWorkflowRepository)
        assertThat(persistenceBean).isSameAs(hostWorkflowPersistence)
        assertThat(applicationContext.getBeansOfType(MongoTemplate::class.java)).hasSize(1)
        assertThat(applicationContext.getBean(MongoTemplate::class.java)).isSameAs(templateBean)
        assertThat(applicationContext.getBean(WorkflowRepository::class.java)).isSameAs(repositoryBean)
        assertThat(applicationContext.getBean(WorkflowPersistence::class.java)).isSameAs(persistenceBean)
        assertThat(hostTemplate.converter).isSameAs(hostConverter)
        assertThat(hostTemplate.hasReadPreference())
            .describedAs("Constructing the prototype must not configure the host template")
            .isFalse()
        assertThat(access.converter.mappingContext).isSameAs(hostConverter.mappingContext)
        assertThat(access.template).isNotSameAs(hostTemplate)
        assertThat(access.converter).isNotSameAs(hostConverter)
        assertThat(access.workflows).isNotSameAs(hostWorkflowPersistence)
        assertThat(access.template.hasReadPreference()).isFalse()

        val hostIdentifier = WorkflowIdentifier(UUID.randomUUID().toString())
        val hostOnlyModel = HostOnlyWorkflowModel("host remains permissive")
        hostRepository.save(PrototypeHostDocument(hostIdentifier.value, hostOnlyModel))
        assertThat(hostRepository.findById(hostIdentifier.value).orElseThrow().model)
            .isEqualTo(hostOnlyModel)

        hostWorkflowPersistence.create(hostOnlyModel, hostIdentifier)
        val prototypeFailure = catchFailure {
            access.workflows.find(hostIdentifier)
        }
        assertUnknownType(
            prototypeFailure,
            TypeRole.MODEL,
            HostOnlyWorkflowModel::class.java.name,
        )
    }

    @Test
    fun `D12 prototype persistence joins host transaction and rolls back`() {
        val loader = WitnessClassLoader()
        val registry = prototypeRegistry(loader, *allowedPrototypeEntries())
        val access = prototypeAccess(registry)
        val prototypeIdentifier = WorkflowIdentifier(UUID.randomUUID().toString())
        val hostIdentifier = WorkflowIdentifier(UUID.randomUUID().toString())
        val model = prototypeModel()
        val hostModel = HostOnlyWorkflowModel("host transaction")
        val transaction = TransactionTemplate(MongoTransactionManager(databaseFactory))

        transaction.executeWithoutResult { status ->
            hostWorkflowPersistence.create(hostModel, hostIdentifier)
            access.workflows.create(model, prototypeIdentifier)
            assertThat(hostWorkflowPersistence.find(hostIdentifier)?.model).isEqualTo(hostModel)
            assertThat(access.workflows.find(prototypeIdentifier)?.model).isEqualTo(model)
            status.setRollbackOnly()
        }

        assertThat(
            workflowCollection(hostTemplate).countDocuments(eq("_id", hostIdentifier.value))
        ).isZero()
        assertThat(
            workflowCollection(hostTemplate).countDocuments(eq("_id", prototypeIdentifier.value))
        ).isZero()
        assertThat(hostWorkflowPersistence.find(hostIdentifier)).isNull()
        assertThat(access.workflows.find(prototypeIdentifier)).isNull()
    }

    @Test
    fun `prototype type queries honor the configured type key`() {
        val loader = WitnessClassLoader()
        val access = prototypeAccess(prototypeRegistry(loader, *allowedPrototypeEntries()), "@type")
        val subtypeId = WorkflowIdentifier(UUID.randomUUID().toString())
        val otherId = WorkflowIdentifier(UUID.randomUUID().toString())
        val subtype = PrototypeWorkflowSubtype("matching-subtype", "custom-key")
        access.workflows.create(subtype, subtypeId)
        access.workflows.create(prototypeModel(), otherId)
        val persisted = requireNotNull(workflowCollection(access.template).find(eq("_id", subtypeId.value)).first())
        val model = persisted.get("model", Document::class.java)
        assertThat(model.getString("@type")).isEqualTo(PrototypeWorkflowSubtype::class.java.name)
        assertThat(model.containsKey("_class")).isFalse()
        assertThat(access.workflows.find(subtypeId)?.model).isEqualTo(subtype)
        val results = access.workflows.findAll(FlowQuery.of {
            where { get(WorkflowData::model).isType(PrototypeWorkflowSubtype::class) }
        })
        assertThat(results.items.map { it.id }).containsExactly(subtypeId.value)
        assertThat(loader.events).isEmpty()
    }
    @ParameterizedTest
    @ValueSource(strings = ["_class", "@type"])
    fun `prototype subtype queries use only their own registry inventory and aliases`(typeKey: String) {
        val loader = WitnessClassLoader()
        val access = prototypeAccess(prototypeRegistry(loader, *allowedPrototypeEntries()), typeKey)
        val modelId = WorkflowIdentifier(UUID.randomUUID().toString())
        val subtypeId = WorkflowIdentifier(UUID.randomUUID().toString())
        val hostId = WorkflowIdentifier(UUID.randomUUID().toString())
        val model = prototypeModel()
        val subtype = PrototypeWorkflowSubtype("registered-subtype", "own-inventory")
        val hostSubtype = PrototypeHostOnlySubtype("host-only-assignable")
        hostWorkflowPersistence.create(hostSubtype, hostId)
        assertThat(hostWorkflowPersistence.find(hostId)?.model).isEqualTo(hostSubtype)
        access.workflows.create(model, modelId)
        access.workflows.create(subtype, subtypeId)
        val collection = workflowCollection(access.template)
        // Align the host-written discriminator with this isolated context's configured layout.
        collection.updateOne(eq("_id", hostId.value), set("model.$typeKey", PrototypeHostOnlySubtype::class.java.name))
        val hostRaw = requireNotNull(collection.find(eq("_id", hostId.value)).first())
        assertThat(hostRaw.get("model", Document::class.java).getString(typeKey))
            .isEqualTo(PrototypeHostOnlySubtype::class.java.name)
        listOf(false, true).forEach { logicalAliases ->
            if (logicalAliases) {
                collection.updateOne(eq("_id", modelId.value), set("model.$typeKey", MODEL_TYPE_ALIAS))
                collection.updateOne(eq("_id", subtypeId.value), set("model.$typeKey", SUBTYPE_ALIAS))
            }
            assertThat(access.workflows.find(modelId)?.model).isEqualTo(model)
            assertThat(access.workflows.find(subtypeId)?.model).isEqualTo(subtype)
            val interfaceMatches = access.workflows.findAll(FlowQuery.of {
                where { get(WorkflowData::model).isType(PrototypeWorkflowModelType::class) }
            })
            assertThat(interfaceMatches.items.map { it.id }).containsExactlyInAnyOrder(modelId.value, subtypeId.value)
            val exactMatches = access.workflows.findAll(FlowQuery.of {
                where { get(WorkflowData::model).isType(PrototypeWorkflowSubtype::class) }
            })
            assertThat(exactMatches.items.map { it.id }).containsExactly(subtypeId.value)
            val hostMatches = access.workflows.findAll(FlowQuery.of {
                where { get(WorkflowData::model).isType(PrototypeHostOnlySubtype::class) }
            })
            assertThat(hostMatches.items).isEmpty()
        }
        assertUnknownType(catchFailure { access.workflows.find(hostId) }, TypeRole.MODEL,
            PrototypeHostOnlySubtype::class.java.name)
        assertThat(loader.events).isEmpty()
    }
    private fun prototypeAccess(
        registry: de.lise.fluxflow.reflection.types.TypeRegistry,
        typeKey: String = "_class",
    ): PrototypeFluxFlowMongoAccess = PrototypeFluxFlowMongoAccess(
        hostTemplate = hostTemplate,
        databaseFactory = databaseFactory,
        registry = registry,
        typeMapperFactory = typeMapperFactory,
        typeKey = typeKey,
    )

    private fun registryWithAmbiguousValueWriteAliases() = prototypeRegistry(
        WitnessClassLoader(),
        prototypeEntry(
            TypeRole.MODEL,
            PrototypeWorkflowModel::class.java.name,
            PrototypeWorkflowModel::class.java.name,
        ),
        prototypeEntry(
            TypeRole.VALUE,
            LEGACY_VALUE_ALIAS,
            PrototypeWorkflowValue::class.java.name,
        ),
        prototypeEntry(
            TypeRole.VALUE,
            RENAMED_VALUE_ALIAS,
            PrototypeWorkflowValue::class.java.name,
        ),
    )

    private fun insertRaw(template: MongoTemplate, document: Document) {
        workflowCollection(template).insertOne(document)
    }

    private fun workflowCollection(template: MongoTemplate) =
        template.getCollection(template.getCollectionName(WorkflowDocument::class.java))

    private fun catchFailure(action: () -> Unit): Throwable =
        runCatching(action).exceptionOrNull()
            ?: throw AssertionError("Expected the guarded Mongo read to fail")
}
