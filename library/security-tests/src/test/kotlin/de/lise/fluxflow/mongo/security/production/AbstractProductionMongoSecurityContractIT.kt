package de.lise.fluxflow.mongo.security.production

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.client.model.Updates.unset
import de.fluxflow.flowquery.expression.ExpressionExtensions.Types.asType
import de.fluxflow.flowquery.expression.ExpressionExtensions.Types.isType
import de.fluxflow.flowquery.query.FlowQuery
import de.lise.fluxflow.api.bootstrapping.BootstrapAction
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.mongo.FluxFlowMongoTemplateCustomizer
import de.lise.fluxflow.mongo.FluxFlowMongoTypeMapperFactory
import de.lise.fluxflow.mongo.FluxFlowMongoTypeAliases
import de.lise.fluxflow.mongo.continuation.history.ContinuationRecordDocument
import de.lise.fluxflow.mongo.flowquery.repository.MongoFlowQueryRepository
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.migration.MigrationDocument
import de.lise.fluxflow.mongo.security.baseline.WITNESS_NAME
import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader
import de.lise.fluxflow.mongo.security.fixtures.HostOnlyWorkflowModel
import de.lise.fluxflow.mongo.security.fixtures.MODEL_TYPE_ALIAS
import de.lise.fluxflow.mongo.security.fixtures.SecurityHostDocument
import de.lise.fluxflow.mongo.security.fixtures.SecurityHostRepository
import de.lise.fluxflow.mongo.security.fixtures.SecurityTestWorkflowModel
import de.lise.fluxflow.mongo.security.fixtures.SecurityTestWorkflowModelType
import de.lise.fluxflow.mongo.security.fixtures.SecurityTestWorkflowSubtype
import de.lise.fluxflow.mongo.security.fixtures.SecurityTestWorkflowValue
import de.lise.fluxflow.mongo.security.fixtures.SUBTYPE_ALIAS
import de.lise.fluxflow.mongo.security.fixtures.VALUE_TYPE_ALIAS
import de.lise.fluxflow.mongo.security.fixtures.assertUnknownType
import de.lise.fluxflow.mongo.security.fixtures.securityTestEntry
import de.lise.fluxflow.mongo.security.fixtures.securityTestModel
import de.lise.fluxflow.mongo.security.fixtures.securityTestRegistry
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.definition.StepDefinitionDocument
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.step.StepPersistence
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.definition.DataDefinitionData
import de.lise.fluxflow.persistence.step.definition.StepDefinitionData
import de.lise.fluxflow.persistence.step.definition.StepDefinitionPersistence
import de.lise.fluxflow.persistence.workflow.query.WorkflowDataQuery
import de.lise.fluxflow.query.pagination.PaginationRequest
import de.lise.fluxflow.persistence.workflow.WorkflowData
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import de.lise.fluxflow.reflection.types.TypeManifestException
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.SimpleApplicationEventMulticaster
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.context.support.GenericApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.MongoTransactionManager
import org.springframework.data.mongodb.core.mapping.event.AfterLoadEvent
import org.springframework.data.mongodb.core.query.Query
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

abstract class AbstractProductionMongoSecurityContractIT {
    @Autowired
    private lateinit var applicationContext: ConfigurableApplicationContext

    @Autowired
    private lateinit var hostTemplate: MongoTemplate

    @Autowired
    private lateinit var databaseFactory: MongoDatabaseFactory

    @Autowired
    private lateinit var fluxFlowMongoAccess: FluxFlowMongoAccess

    @Autowired
    private lateinit var typeMapperFactory: FluxFlowMongoTypeMapperFactory

    @Autowired
    private lateinit var workflows: WorkflowPersistence

    @Autowired
    private lateinit var steps: StepPersistence

    @Autowired
    private lateinit var jobs: JobPersistence

    @Autowired
    private lateinit var stepDefinitions: StepDefinitionPersistence

    @Autowired
    private lateinit var workflowFlowQueries: MongoFlowQueryRepository<WorkflowDocument>

    @Autowired
    @Qualifier("migrateToTypeRecordsBootstrapAction")
    private lateinit var typeRecordMigration: BootstrapAction

    @Autowired
    private lateinit var hostRepository: SecurityHostRepository

    @Autowired
    @Qualifier("productionWitnessClassLoader")
    private lateinit var witnessLoader: WitnessClassLoader

    @BeforeEach
    fun clearMongoCollections() {
        assertThat(witnessLoader.events).isEmpty()
        listOf(
            WorkflowDocument::class.java,
            StepDocument::class.java,
            JobDocument::class.java,
            StepDefinitionDocument::class.java,
            ContinuationRecordDocument::class.java,
            MigrationDocument::class.java,
            SecurityHostDocument::class.java,
        ).forEach { type ->
            if (hostTemplate.collectionExists(type)) {
                hostTemplate.remove(Query(), type)
            }
        }
    }

    @Test
    fun `R03 D02 production workflow reads reject an unregistered model before materialization`() {
        val identifier = WorkflowIdentifier(UUID.randomUUID().toString())
        workflows.create(securityTestModel(), identifier)
        workflowCollection().updateOne(
            eq("_id", identifier.value),
            combine(
                set("model", Document("_class", WITNESS_NAME)),
                set("modelType", WITNESS_NAME),
            ),
        )
        assertThat(witnessLoader.events).isEmpty()

        val failure = runCatching { workflows.find(identifier) }.exceptionOrNull()
            ?: throw AssertionError("Expected the production Mongo read to fail closed")

        assertUnknownType(failure, TypeRole.MODEL, WITNESS_NAME)
        assertThat(witnessLoader.events)
            .describedAs("R03 must reject the model type before initializing or constructing it")
            .isEmpty()
        assertThat(
            fluxFlowMongoAccess.template
                .getCollection(fluxFlowMongoAccess.template.getCollectionName(WorkflowDocument::class.java))
                .countDocuments(eq("_id", identifier.value))
        ).isEqualTo(1)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `D01 findAll pagination and FlowQuery reject an unregistered model`() {
        val operations: List<(String) -> Unit> = listOf(
            { workflows.findAll() },
            { workflows.findAll(WorkflowDataQuery(null, emptyList(), PaginationRequest(0, 10))) },
            { id ->
                workflows.findAll(
                    FlowQuery.of { where { get(WorkflowData::id).isEqual(id) } }
                )
            },
        )

        operations.forEach { operation ->
            val id = UUID.randomUUID().toString()
            val identifier = WorkflowIdentifier(id)
            workflows.create(securityTestModel(), identifier)
            workflowCollection().updateOne(
                eq("_id", id),
                combine(
                    set("model", Document("_class", WITNESS_NAME)),
                    set("modelType", WITNESS_NAME),
                ),
            )

            assertUnknownType(catchFailure { operation(id) }, TypeRole.MODEL, WITNESS_NAME)
            workflowCollection().deleteOne(eq("_id", id))
        }
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `D03 production workflow reads reject nested unregistered and wrong-role types`() {
        val unregisteredId = UUID.randomUUID().toString()
        workflows.create(securityTestModel(), WorkflowIdentifier(unregisteredId))
        workflowCollection().updateOne(
            eq("_id", unregisteredId),
            set("model.nested.0.fqcn._class", WITNESS_NAME),
        )

        val unregisteredFailure = catchFailure {
            workflows.find(WorkflowIdentifier(unregisteredId))
        }
        assertUnknownType(unregisteredFailure, TypeRole.VALUE, WITNESS_NAME)

        val wrongRoleId = UUID.randomUUID().toString()
        workflows.create(securityTestModel(), WorkflowIdentifier(wrongRoleId))
        workflowCollection().updateOne(
            eq("_id", wrongRoleId),
            set("model.nested.0.alias._class", SUBTYPE_ALIAS),
        )

        val wrongRoleFailure = catchFailure {
            workflows.find(WorkflowIdentifier(wrongRoleId))
        }
        assertUnknownType(wrongRoleFailure, TypeRole.VALUE, SUBTYPE_ALIAS)
        assertThat(witnessLoader.events)
            .describedAs("Nested types must be rejected before materialization")
            .isEmpty()
    }

    @Test
    fun `D08 production converter rejects malformed unknown and wrong-role aliases`() {
        val converter = fluxFlowMongoAccess.converter

        val unknown = catchFailure {
            converter.read(
                WorkflowDocument::class.java,
                rawWorkflow("unknown", "not.registered.Model"),
            )
        }
        assertUnknownType(unknown, TypeRole.MODEL, "not.registered.Model")

        val wrongRole = catchFailure {
            converter.read(
                WorkflowDocument::class.java,
                rawWorkflow("wrong-role", VALUE_TYPE_ALIAS),
            )
        }
        assertUnknownType(wrongRole, TypeRole.MODEL, VALUE_TYPE_ALIAS)

        assertThatThrownBy {
            converter.read(
                WorkflowDocument::class.java,
                rawWorkflow("empty", ""),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("must not be empty")

        assertThatThrownBy {
            converter.read(
                WorkflowDocument::class.java,
                rawWorkflow("non-string", 42),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("must be a string")

        val unknownRoot = "not.registered.Root"
        assertThatThrownBy {
            converter.read(
                WorkflowDocument::class.java,
                Document("_id", "unknown-root")
                    .append("model", "safe scalar")
                    .append("modelType", String::class.java.name)
                    .append("_class", unknownRoot),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining(unknownRoot)

        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `D08 aliases shared by different Mongo roles and classes fail during construction`() {
        val ambiguousAlias = "ambiguous"
        val registry = securityTestRegistry(
            WitnessClassLoader(),
            securityTestEntry(
                TypeRole.MODEL,
                ambiguousAlias,
                SecurityTestWorkflowModel::class.java.name,
            ),
            securityTestEntry(
                TypeRole.VALUE,
                ambiguousAlias,
                SecurityTestWorkflowValue::class.java.name,
            ),
        )

        assertThatThrownBy {
            FluxFlowMongoTypeAliases(registry, emptySet(), emptySet())
        }.isInstanceOf(TypeManifestException::class.java)
            .hasMessageContaining(ambiguousAlias)
    }

    @Test
    fun `D08 D09 production persistence accepts registered FQCN and logical aliases`() {
        val identifier = WorkflowIdentifier(UUID.randomUUID().toString())
        val model = securityTestModel()
        workflows.create(model, identifier)

        val persisted = requireNotNull(workflowCollection().find(eq("_id", identifier.value)).first())
        val persistedModel = persisted.get("model", Document::class.java)
        assertThat(persistedModel.getString("_class"))
            .isEqualTo(SecurityTestWorkflowModel::class.java.name)
        assertThat(persistedModel.getString("mapped_name")).isEqualTo(model.name)
        assertThat(persistedModel.containsKey("name")).isFalse()
        assertThat(persistedModel.getString("converted"))
            .isEqualTo("converted:custom-conversion")

        val aliasUpdate = workflowCollection().updateOne(
            eq("_id", identifier.value),
            combine(
                set("model._class", MODEL_TYPE_ALIAS),
                set("model.nested.0.alias._class", VALUE_TYPE_ALIAS),
            ),
        )
        assertThat(aliasUpdate.matchedCount).isEqualTo(1)
        assertThat(aliasUpdate.modifiedCount).isEqualTo(1)

        assertThat(workflows.find(identifier)?.model).isEqualTo(model)

        val subtypeId = WorkflowIdentifier(UUID.randomUUID().toString())
        val subtype = SecurityTestWorkflowSubtype("allowed subtype", "preserved")
        workflows.create(subtype, subtypeId)
        assertThat(workflows.find(subtypeId)?.model)
            .isEqualTo(subtype)
            .isInstanceOf(SecurityTestWorkflowSubtype::class.java)
    }

    @Test
    fun `D09 null scalar collection and map workflow models round-trip in production`() {
        listOf<Any?>(
            null,
            "scalar",
            listOf("one", 2, null),
            linkedMapOf("string" to "value", "null" to null),
        ).forEach { model ->
            val identifier = WorkflowIdentifier(UUID.randomUUID().toString())
            workflows.create(model, identifier)

            assertThat(workflows.find(identifier)?.model).isEqualTo(model)
        }
    }

    @Test
    fun `D10 type queries use only registered model subtypes`() {
        val modelId = WorkflowIdentifier(UUID.randomUUID().toString())
        val subtypeId = WorkflowIdentifier(UUID.randomUUID().toString())
        workflows.create(securityTestModel(), modelId)
        workflows.create(SecurityTestWorkflowSubtype("subtype", "registered"), subtypeId)

        val results = workflowFlowQueries.find(WorkflowDocument::class.java) {
            where {
                get(WorkflowDocument::model).isType(SecurityTestWorkflowModelType::class)
            }
        }

        assertThat(results.map { it.id })
            .containsExactlyInAnyOrder(modelId.value, subtypeId.value)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `D11 production access keeps host template converter and repository isolated`() {
        val hostConverter = hostTemplate.converter

        assertThat(applicationContext.getBeansOfType(MongoTemplate::class.java)).hasSize(1)
        assertThat(applicationContext.getBean(MongoTemplate::class.java)).isSameAs(hostTemplate)
        assertThat(fluxFlowMongoAccess.template).isNotSameAs(hostTemplate)
        assertThat(fluxFlowMongoAccess.converter).isNotSameAs(hostConverter)
        assertThat(fluxFlowMongoAccess.converter.mappingContext)
            .isSameAs(hostConverter.mappingContext)
        assertThat(hostTemplate.hasReadPreference()).isTrue()
        assertThat(fluxFlowMongoAccess.template.readPreference)
            .isEqualTo(hostTemplate.readPreference)

        val hostDocument = SecurityHostDocument(
            UUID.randomUUID().toString(),
            HostOnlyWorkflowModel("host remains permissive"),
        )
        hostRepository.save(hostDocument)
        assertThat(hostRepository.findById(hostDocument.id).orElseThrow())
            .isEqualTo(hostDocument)

        val workflowId = WorkflowIdentifier(UUID.randomUUID().toString())
        hostTemplate.save(
            WorkflowDocument(
                workflowId.value,
                HostOnlyWorkflowModel("host-written workflow"),
                HostOnlyWorkflowModel::class.java.name,
            )
        )
        val restrictedFailure = catchFailure { workflows.find(workflowId) }
        assertUnknownType(
            restrictedFailure,
            TypeRole.MODEL,
            HostOnlyWorkflowModel::class.java.name,
        )
    }

    @Test
    fun `D12 production persistence joins host transaction and rolls back`() {
        val workflowId = WorkflowIdentifier(UUID.randomUUID().toString())
        val hostDocument = SecurityHostDocument(
            UUID.randomUUID().toString(),
            HostOnlyWorkflowModel("host transaction"),
        )
        val transaction = TransactionTemplate(MongoTransactionManager(databaseFactory))

        transaction.executeWithoutResult { status ->
            hostRepository.save(hostDocument)
            workflows.create(securityTestModel(), workflowId)

            assertThat(hostRepository.findById(hostDocument.id).orElseThrow())
                .isEqualTo(hostDocument)
            assertThat(workflows.find(workflowId)?.model).isEqualTo(securityTestModel())
            status.setRollbackOnly()
        }

        assertThat(hostRepository.findById(hostDocument.id)).isEmpty
        assertThat(workflowCollection().countDocuments(eq("_id", workflowId.value))).isZero()
        assertThat(workflows.find(workflowId)).isNull()
    }

    @Test
    fun `D13 parallel production accesses keep registry contexts isolated and fail closed`() {
        val contextAAlias = "d13-context-a-model"
        val contextBAlias = "d13-context-b-model"
        val contextALoader = WitnessClassLoader()
        val contextBLoader = WitnessClassLoader()
        val contextAAccess = productionAccess(
            securityTestRegistry(
                contextALoader,
                securityTestEntry(
                    TypeRole.MODEL,
                    contextAAlias,
                    SecurityTestWorkflowSubtype::class.java.name,
                ),
            )
        )
        val contextBAccess = productionAccess(
            securityTestRegistry(
                contextBLoader,
                securityTestEntry(
                    TypeRole.MODEL,
                    contextBAlias,
                    HostOnlyWorkflowModel::class.java.name,
                ),
            )
        )
        assertThat(contextAAccess.converter).isNotSameAs(contextBAccess.converter)
        assertThat(contextAAccess.template).isNotSameAs(contextBAccess.template)

        val contextAId = UUID.randomUUID().toString()
        val contextBId = UUID.randomUUID().toString()
        val unknownId = UUID.randomUUID().toString()
        val contextAModel = SecurityTestWorkflowSubtype("context A", "isolated")
        val contextBModel = HostOnlyWorkflowModel("context B")
        insertRaw(
            WorkflowDocument::class.java,
            rawWorkflow(
                contextAId,
                contextAAlias,
                Document("name", contextAModel.name)
                    .append("subtypeValue", contextAModel.subtypeValue),
            ),
        )
        insertRaw(
            WorkflowDocument::class.java,
            rawWorkflow(
                contextBId,
                contextBAlias,
                Document("value", contextBModel.value),
            ),
        )
        insertRaw(WorkflowDocument::class.java, rawWorkflow(unknownId, WITNESS_NAME))

        val start = CountDownLatch(1)
        val failures = ConcurrentLinkedQueue<Throwable>()
        val executor = Executors.newFixedThreadPool(8)
        val futures = (0 until 8).map { worker ->
            executor.submit {
                start.await()
                repeat(20) {
                    try {
                        if (worker % 2 == 0) {
                            assertThat(contextAAccess.findWorkflow(contextAId)?.model)
                                .isEqualTo(contextAModel)
                            assertUnknownType(
                                catchFailure { contextAAccess.findWorkflow(contextBId) },
                                TypeRole.MODEL,
                                contextBAlias,
                            )
                            assertUnknownType(
                                catchFailure { contextAAccess.findWorkflow(unknownId) },
                                TypeRole.MODEL,
                                WITNESS_NAME,
                            )
                        } else {
                            assertThat(contextBAccess.findWorkflow(contextBId)?.model)
                                .isEqualTo(contextBModel)
                            assertUnknownType(
                                catchFailure { contextBAccess.findWorkflow(contextAId) },
                                TypeRole.MODEL,
                                contextAAlias,
                            )
                            assertUnknownType(
                                catchFailure { contextBAccess.findWorkflow(unknownId) },
                                TypeRole.MODEL,
                                WITNESS_NAME,
                            )
                        }
                    } catch (failure: Throwable) {
                        failures.add(failure)
                    }
                }
            }
        }

        try {
            start.countDown()
            futures.forEach { it.get(30, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        assertThat(failures)
            .describedAs("Registry-bound Mongo access must stay isolated under parallel reads")
            .isEmpty()
        assertUnknownType(
            catchFailure { fluxFlowMongoAccess.findWorkflow(contextAId) },
            TypeRole.MODEL,
            contextAAlias,
        )
        assertUnknownType(
            catchFailure { fluxFlowMongoAccess.findWorkflow(contextBId) },
            TypeRole.MODEL,
            contextBAlias,
        )
        assertThat(contextALoader.events).isEmpty()
        assertThat(contextBLoader.events).isEmpty()
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `D04 production step reads reject types in legacy and current value fields`() {
        listOf(
            "data.payload" to "dataEntries",
            "metadata.payload" to "metadataEntries",
            "dataEntries.values.payload" to null,
            "metadataEntries.values.payload" to null,
        ).forEach { (path, legacyEntries) ->
            val id = ObjectId().toHexString()
            val workflowId = UUID.randomUUID().toString()
            steps.create(
                StepData(
                    id,
                    workflowId,
                    "test-step",
                    "1",
                    mapOf("payload" to SecurityTestWorkflowValue("safe")),
                    Status.Active,
                    mapOf("payload" to SecurityTestWorkflowValue("safe")),
                )
            )
            val updates = mutableListOf(set(path, typedWitness()))
            legacyEntries?.let { updates += unset(it) }
            collection(StepDocument::class.java).updateOne(
                eq("_id", ObjectId(id)),
                combine(updates),
            )

            val failure = catchFailure {
                steps.findForWorkflowAndId(
                    WorkflowIdentifier(workflowId),
                    StepIdentifier(id),
                )
            }
            assertUnknownType(failure, TypeRole.VALUE, WITNESS_NAME)
        }
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `D04 production job reads reject types in legacy and current parameter fields`() {
        listOf(
            "parameters.payload" to true,
            "parameterEntries.values.payload" to false,
        ).forEach { (path, legacy) ->
            val id = ObjectId().toHexString()
            val workflowId = UUID.randomUUID().toString()
            jobs.create(
                JobData(
                    id,
                    workflowId,
                    "test-job",
                    mapOf("payload" to SecurityTestWorkflowValue("safe")),
                    Instant.parse("2026-09-10T10:00:00Z"),
                    null,
                    JobStatus.Scheduled,
                )
            )
            val updates = mutableListOf(set(path, typedWitness()))
            if (legacy) updates += unset("parameterEntries")
            collection(JobDocument::class.java).updateOne(
                eq("_id", ObjectId(id)),
                combine(updates),
            )

            val failure = catchFailure {
                jobs.findForWorkflowAndId(
                    WorkflowIdentifier(workflowId),
                    JobIdentifier(id),
                )
            }
            assertUnknownType(failure, TypeRole.VALUE, WITNESS_NAME)
        }
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `D04 production step definition reads reject nested metadata types`() {
        listOf(
            "metadata.values.payload",
            "data.0.metadata.values.payload",
        ).forEachIndexed { index, path ->
            val kind = "malicious-step-definition-$index"
            stepDefinitions.save(
                StepDefinitionData(
                    kind,
                    "1",
                    mapOf("payload" to SecurityTestWorkflowValue("safe")),
                    listOf(
                        DataDefinitionData(
                            "data",
                            String::class.java.name,
                            mapOf("payload" to SecurityTestWorkflowValue("safe")),
                            false,
                        )
                    ),
                )
            )
            collection(StepDefinitionDocument::class.java).updateOne(
                and(eq("kind", kind), eq("version", "1")),
                set(path, typedWitness()),
            )

            val failure = catchFailure {
                stepDefinitions.findForKindAndVersion(kind, "1")
            }
            assertUnknownType(failure, TypeRole.VALUE, WITNESS_NAME)
        }
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `D05 legacy step bootstrap rejects a type before conversion and leaves BSON unchanged`() {
        val id = ObjectId()
        val workflowId = UUID.randomUUID().toString()
        steps.create(
            StepData(
                id.toHexString(), workflowId, "legacy-step", "1",
                mapOf("payload" to SecurityTestWorkflowValue("safe")), Status.Active, emptyMap(),
            )
        )
        collection(StepDocument::class.java).updateOne(
            eq("_id", id),
            combine(
                unset("dataEntries"),
                unset("metadataEntries"),
                set("data.payload", typedWitness()),
            ),
        )
        val before = requireNotNull(collection(StepDocument::class.java).find(eq("_id", id)).first())

        assertThatThrownBy { typeRecordMigration.setup() }
            .isInstanceOf(de.lise.fluxflow.migration.MigrationError::class.java)

        val persisted = requireNotNull(
            collection(StepDocument::class.java).find(eq("_id", id)).first()
        )
        assertThat(persisted.containsKey("dataEntries")).isFalse()
        assertThat(persisted["data"]).isEqualTo(before["data"])
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `D05 legacy job bootstrap rejects a type before conversion and leaves BSON unchanged`() {
        val id = ObjectId()
        val workflowId = UUID.randomUUID().toString()
        jobs.create(
            JobData(
                id.toHexString(), workflowId, "legacy-job",
                mapOf("payload" to SecurityTestWorkflowValue("safe")),
                Instant.parse("2026-09-10T10:00:00Z"), null, JobStatus.Scheduled,
            )
        )
        collection(JobDocument::class.java).updateOne(
            eq("_id", id),
            combine(
                unset("parameterEntries"),
                set("parameters.payload", typedWitness()),
            ),
        )
        val before = requireNotNull(collection(JobDocument::class.java).find(eq("_id", id)).first())

        assertThatThrownBy { typeRecordMigration.setup() }
            .isInstanceOf(de.lise.fluxflow.migration.MigrationError::class.java)

        val persisted = requireNotNull(
            collection(JobDocument::class.java).find(eq("_id", id)).first()
        )
        assertThat(persisted.containsKey("parameterEntries")).isFalse()
        assertThat(persisted["parameters"]).isEqualTo(before["parameters"])
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `D06 production aggregation projection rejects nested types before materialization`() {
        val id = UUID.randomUUID().toString()
        workflows.create(securityTestModel(), WorkflowIdentifier(id))
        workflowCollection().updateOne(
            eq("_id", id),
            set("model.nested.0.alias._class", SUBTYPE_ALIAS),
        )

        val failure = catchFailure {
            workflowFlowQueries.find(SecurityTestWorkflowModel::class.java) {
                where { get(WorkflowDocument::id).isEqual(id) }
                    .project {
                        get(WorkflowDocument::model).asType(SecurityTestWorkflowModel::class)
                    }
            }
        }

        assertUnknownType(failure, TypeRole.VALUE, SUBTYPE_ALIAS)
        assertThat(witnessLoader.events).isEmpty()
    }

    @Test
    fun `D07 production reads stay guarded without synchronous lifecycle events`() {
        fluxFlowMongoAccess.template.setEntityLifecycleEventsEnabled(false)
        try {
            val disabledId = UUID.randomUUID().toString()
            persistAndTamperWorkflow(disabledId, WITNESS_NAME)
            assertUnknownType(
                catchFailure { workflows.find(WorkflowIdentifier(disabledId)) },
                TypeRole.MODEL,
                WITNESS_NAME,
            )
        } finally {
            fluxFlowMongoAccess.template.setEntityLifecycleEventsEnabled(true)
        }

        val queuedEvents = CopyOnWriteArrayList<Runnable>()
        val asynchronousContext = GenericApplicationContext().apply {
            beanFactory.registerSingleton(
                AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
                SimpleApplicationEventMulticaster().apply {
                    setTaskExecutor { task -> queuedEvents += task }
                },
            )
            addApplicationListener(ApplicationListener<ApplicationEvent> { })
            refresh()
        }
        try {
            fluxFlowMongoAccess.template.setApplicationContext(asynchronousContext)
            val asynchronousId = UUID.randomUUID().toString()
            persistAndTamperWorkflow(asynchronousId, WITNESS_NAME)
            queuedEvents.clear()

            val failure = catchFailure {
                workflows.findAll(
                    FlowQuery.of {
                        where { get(WorkflowData::id).isEqual(asynchronousId) }
                    }
                )
            }
            assertUnknownType(failure, TypeRole.MODEL, WITNESS_NAME)
            assertThat(queuedEvents).isNotEmpty()
        } finally {
            fluxFlowMongoAccess.template.setApplicationContext(applicationContext)
            asynchronousContext.close()
        }

        val swallowedErrors = AtomicInteger()
        val swallowingContext = GenericApplicationContext().apply {
            addApplicationListener(ApplicationListener<ApplicationEvent> { event ->
                if (event is AfterLoadEvent<*>) {
                    runCatching { throw IllegalStateException("listener rejection") }
                        .onFailure { swallowedErrors.incrementAndGet() }
                }
            })
            refresh()
        }
        try {
            fluxFlowMongoAccess.template.setApplicationContext(swallowingContext)
            val swallowedId = UUID.randomUUID().toString()
            persistAndTamperWorkflow(swallowedId, WITNESS_NAME)

            assertUnknownType(
                catchFailure { workflows.find(WorkflowIdentifier(swallowedId)) },
                TypeRole.MODEL,
                WITNESS_NAME,
            )
            assertThat(swallowedErrors.get()).isEqualTo(1)
        } finally {
            fluxFlowMongoAccess.template.setApplicationContext(applicationContext)
            swallowingContext.close()
        }

        assertThat(witnessLoader.events).isEmpty()
    }

    private fun persistAndTamperWorkflow(id: String, modelType: String) {
        workflows.create(securityTestModel(), WorkflowIdentifier(id))
        workflowCollection().updateOne(
            eq("_id", id),
            combine(
                set("model", Document("_class", modelType)),
                set("modelType", modelType),
            ),
        )
    }

    private fun rawWorkflow(
        id: String,
        modelType: Any?,
        additionalModelFields: Document = Document(),
    ): Document = Document("_id", id)
        .append("model", Document(additionalModelFields).append("_class", modelType))
        .append("modelType", modelType as? String)
        .append("_class", WorkflowDocument::class.java.name)

    private fun typedWitness(): Document = typedAlias(WITNESS_NAME)

    private fun typedAlias(alias: String): Document = Document("_class", alias)

    private fun insertRaw(type: Class<*>, document: Document) {
        collection(type).insertOne(document)
    }

    private fun collection(type: Class<*>) = fluxFlowMongoAccess.template.getCollection(
        fluxFlowMongoAccess.template.getCollectionName(type)
    )

    private fun workflowCollection() = collection(WorkflowDocument::class.java)

    private fun productionAccess(registry: TypeRegistry): FluxFlowMongoAccess =
        FluxFlowMongoAccess(
            hostTemplate,
            registry,
            typeMapperFactory,
            applicationContext,
            applicationContext.beanFactory,
            applicationContext.environment,
            applicationContext.getBeanProvider(FluxFlowMongoTemplateCustomizer::class.java),
        )

    private fun FluxFlowMongoAccess.findWorkflow(id: String): WorkflowDocument? =
        template.findById(id, WorkflowDocument::class.java)

    private fun catchFailure(action: () -> Unit): Throwable =
        runCatching(action).exceptionOrNull()
            ?: throw AssertionError("Expected the production Mongo operation to fail closed")
}
