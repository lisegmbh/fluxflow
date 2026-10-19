package de.lise.fluxflow.mongo.security.production

import de.lise.fluxflow.mongo.FluxFlowMongoTypeAliases
import de.lise.fluxflow.mongo.MongoDocumentTypePolicy
import de.lise.fluxflow.mongo.MongoTypeAliasIssue
import de.lise.fluxflow.mongo.MongoTypeAuditInspectionException
import de.lise.fluxflow.mongo.generic.CollectionType
import de.lise.fluxflow.mongo.generic.NullType
import de.lise.fluxflow.mongo.generic.SimpleType
import de.lise.fluxflow.mongo.generic.ValueTypeConverter
import de.lise.fluxflow.mongo.generic.record.JvmTypeMapping
import de.lise.fluxflow.mongo.generic.record.TypeRecordEntry
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.definition.DataDefinitionDocument
import de.lise.fluxflow.mongo.step.definition.StepDefinitionDocument
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.reflection.types.TypeManifestEntry
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.assertj.core.groups.Tuple.tuple
import org.bson.Document
import org.junit.jupiter.api.Test

class MongoDocumentTypePolicySharedIdentityTest {
    @Test
    fun `shared MODEL map is checked at Static field in both insertion orders`() {
        val policy = policy()

        listOf(true, false).forEach { modelFirst ->
            val shared = Document("_class", MODEL_ALIAS)
            val allowedSource = Document("_class", WorkflowDocument::class.java.name)
                .append("model", shared)
            policy.validate(WorkflowDocument::class.java, allowedSource)

            val source = Document("_class", WorkflowDocument::class.java.name)
            if (modelFirst) {
                source["model"] = shared
                source["extra"] = shared
            } else {
                source["extra"] = shared
                source["model"] = shared
            }

            assertThatThrownBy { policy.validate(WorkflowDocument::class.java, source) }
                .describedAs("shared MODEL/Static map (modelFirst=%s)", modelFirst)
                .isInstanceOf(IllegalArgumentException::class.java)

            assertSingleDeviation(policy, WorkflowDocument::class.java, source, "/extra/_class", MODEL_ALIAS)
        }
    }

    @Test
    fun `shared infrastructure map is checked at VALUE field in both insertion orders`() {
        val policy = policy()

        listOf(true, false).forEach { infrastructureFirst ->
            val shared = Document("_class", SimpleType::class.java.name)
                .append("typeName", String::class.java.name)
            val allowedSource = Document("_class", StepDocument::class.java.name)
                .append("dataTypeMap", Document("payload", shared))
            policy.validate(StepDocument::class.java, allowedSource)

            val source = Document("_class", StepDocument::class.java.name)
            if (infrastructureFirst) {
                source["dataTypeMap"] = Document("payload", shared)
                source["data"] = Document("payload", shared)
            } else {
                source["data"] = Document("payload", shared)
                source["dataTypeMap"] = Document("payload", shared)
            }

            assertThatThrownBy { policy.validate(StepDocument::class.java, source) }
                .describedAs("shared infrastructure/VALUE map (infrastructureFirst=%s)", infrastructureFirst)
                .isInstanceOf(IllegalArgumentException::class.java)

            assertSingleDeviation(policy, StepDocument::class.java, source, "/data/payload/_class", SimpleType::class.java.name)
        }
    }

    @Test
    fun `shared List and Array retain StepDefinition data policy against Static field`() {
        val policy = policy()
        val dataDefinition = Document("_class", DataDefinitionDocument::class.java.name)

        listOf(
            "List" to listOf(dataDefinition),
            "Array" to arrayOf(dataDefinition),
        ).forEach { (kind, sharedContainer) ->
            listOf(true, false).forEach { dataFirst ->
                val allowedSource = Document("_class", StepDefinitionDocument::class.java.name)
                    .append("data", sharedContainer)
                policy.validate(StepDefinitionDocument::class.java, allowedSource)

                val source = Document("_class", StepDefinitionDocument::class.java.name)
                if (dataFirst) {
                    source["data"] = sharedContainer
                    source["extra"] = sharedContainer
                } else {
                    source["extra"] = sharedContainer
                    source["data"] = sharedContainer
                }

                assertThatThrownBy { policy.validate(StepDefinitionDocument::class.java, source) }
                    .describedAs("shared %s in StepDefinition data/Static fields (dataFirst=%s)", kind, dataFirst)
                    .isInstanceOf(IllegalArgumentException::class.java)

                assertSingleDeviation(
                    policy,
                    StepDefinitionDocument::class.java,
                    source,
                    "/extra/0/_class",
                    DataDefinitionDocument::class.java.name,
                )
            }
        }
    }

    @Test
    fun `shared map under the same Static policy produces one deterministic audit deviation`() {
        val policy = policy()
        val shared = Document("_class", MODEL_ALIAS)
        val source = Document("_class", WorkflowDocument::class.java.name)
            .append("first", shared)
            .append("second", shared)

        assertThatThrownBy { policy.validate(WorkflowDocument::class.java, source) }
            .isInstanceOf(IllegalArgumentException::class.java)

        assertSingleDeviation(policy, WorkflowDocument::class.java, source, "/second/_class", MODEL_ALIAS)
    }

    @Test
    fun `self-referential Map List and Array terminate during projection validation and alias audit`() {
        val policy = policy(maxDepth = 16, maxNodes = 256)
        val cycles = listOf(
            "Map" to ::selfReferentialMap,
            "List" to ::selfReferentialList,
            "Array" to ::selfReferentialArray,
        )

        cycles.forEach { (kind, newCycle) ->
            policy.validateProjection(SimpleType::class.java, Document("payload", newCycle()))

            val deviations = policy.auditAliases(
                SimpleType::class.java,
                Document("payload", newCycle()),
                ValueTypeConverter(registry())::isRegistered,
            )
            assertThat(deviations)
                .describedAs("%s self-cycle has no discriminator", kind)
                .isEmpty()
        }
    }

    @Test
    fun `invalid siblings preserve read failure order and audit JSON pointer order`() {
        val policy = policy()
        val source = Document("_class", WorkflowDocument::class.java.name)
            .append("first/key~.leaf", Document("_class", "unknown-first"))
            .append("second/key~.leaf", Document("_class", "unknown-second"))

        val readFailure = catchThrowable { policy.validate(WorkflowDocument::class.java, source) }
        assertThat(readFailure).isExactlyInstanceOf(IllegalArgumentException::class.java)
        assertThat(readFailure?.message)
            .isEqualTo("Mongo type metadata is not allowed at 'second/key~.leaf._class'")
        assertThat(readFailure?.suppressed?.map(Throwable::message))
            .containsExactly("Invalid Mongo type at 'second/key~.leaf._class'")

        val deviations = policy.auditAliases(WorkflowDocument::class.java, source, ValueTypeConverter(registry())::isRegistered)
        assertThat(deviations)
            .extracting("path", "issue", "persistedName")
            .containsExactly(
                tuple("/second~1key~0.leaf/_class", MongoTypeAliasIssue.UNREGISTERED, "unknown-second"),
                tuple("/first~1key~0.leaf/_class", MongoTypeAliasIssue.UNREGISTERED, "unknown-first"),
            )
    }

    @Test
    fun `read and audit report their budget paths for the same in-memory graph`() {
        val policy = policy(maxDepth = 2, maxNodes = 32)
        val source = Document("_class", WorkflowDocument::class.java.name)
            .append("model", Document("inner", Document("deep/key~.leaf", "value")))

        val readFailure = catchThrowable { policy.validate(WorkflowDocument::class.java, source) }
        assertThat(readFailure).isExactlyInstanceOf(IllegalArgumentException::class.java)
        assertThat(readFailure?.message)
            .isEqualTo("Mongo document exceeds the maximum depth of 2 at 'model.inner.deep/key~.leaf'")

        val auditFailure = catchThrowable {
            policy.auditAliases(WorkflowDocument::class.java, source, ValueTypeConverter(registry())::isRegistered)
        }
        assertThat(auditFailure).isExactlyInstanceOf(MongoTypeAuditInspectionException::class.java)
        assertThat((auditFailure as MongoTypeAuditInspectionException).path)
            .isEqualTo("/model/inner/deep~1key~0.leaf")
        assertThat(auditFailure.message)
            .isEqualTo("Mongo document exceeds the maximum depth of 2 at '/model/inner/deep~1key~0.leaf'")
    }

    @Test
    fun `legacy SimpleType discriminator does not consume an extra alias audit node`() {
        val policy = policy(maxNodes = 4)
        val simpleType = Document("typeName", String::class.java.name)
        val baseline = Document("_class", StepDocument::class.java.name)
            .append("dataTypeMap", Document("payload", simpleType))

        assertThat(policy.auditAliases(StepDocument::class.java, baseline, ValueTypeConverter(registry())::isRegistered))
            .isEmpty()

        val withInfrastructureDiscriminator = Document("_class", StepDocument::class.java.name)
            .append("dataTypeMap", Document("payload", Document("typeName", String::class.java.name)
                .append("_class", SimpleType::class.java.name)))
        assertThat(
            policy.auditAliases(
                StepDocument::class.java,
                withInfrastructureDiscriminator,
                ValueTypeConverter(registry())::isRegistered,
            )
        ).isEmpty()
    }

    private fun assertSingleDeviation(
        policy: MongoDocumentTypePolicy,
        targetType: Class<*>,
        source: Document,
        expectedPath: String,
        expectedAlias: String,
    ) {
        val deviations = policy.auditAliases(targetType, source, ValueTypeConverter(registry())::isRegistered)
        assertThat(deviations).hasSize(1)
        assertThat(deviations.single().path).isEqualTo(expectedPath)
        assertThat(deviations.single().persistedName).isEqualTo(expectedAlias)
        assertThat(deviations.single().issue).isEqualTo(MongoTypeAliasIssue.DISALLOWED)
    }

    private fun policy(maxDepth: Int = 100, maxNodes: Int = 100_000): MongoDocumentTypePolicy {
        val registry = registry()
        val aliases = FluxFlowMongoTypeAliases(
            registry,
            setOf(
                WorkflowDocument::class.java,
                StepDocument::class.java,
                JobDocument::class.java,
                StepDefinitionDocument::class.java,
            ),
            setOf(
                SimpleType::class.java,
                CollectionType::class.java,
                NullType::class.java,
                JvmTypeMapping::class.java,
                TypeRecordEntry::class.java,
                TypedRecords::class.java,
                DataDefinitionDocument::class.java,
            ),
        )
        return MongoDocumentTypePolicy(
            aliases,
            maxDepth = maxDepth,
            maxNodes = maxNodes,
            isRegisteredJvmValueType = ValueTypeConverter(registry)::isRegistered,
        )
    }

    private fun selfReferentialMap(): Any = linkedMapOf<String, Any?>().apply { put("self", this) }

    private fun selfReferentialList(): Any = mutableListOf<Any?>().apply { add(this) }

    private fun selfReferentialArray(): Any = arrayOfNulls<Any>(1).also { it[0] = it }

    private fun registry(): TypeRegistry = TypeRegistry.create(
        javaClass.classLoader,
        listOf(
            TypeManifestEntry(TypeRole.MODEL, MODEL_ALIAS, ModelFixture::class.java.name, "shared identity test"),
            TypeManifestEntry(TypeRole.VALUE, VALUE_ALIAS, ValueFixture::class.java.name, "shared identity test"),
        ),
    )

    private class ModelFixture
    private class ValueFixture

    private companion object {
        const val MODEL_ALIAS = "shared-identity-model"
        const val VALUE_ALIAS = "shared-identity-value"
    }
}
