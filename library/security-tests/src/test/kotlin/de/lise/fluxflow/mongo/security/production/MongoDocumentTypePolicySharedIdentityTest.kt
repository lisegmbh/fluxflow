package de.lise.fluxflow.mongo.security.production

import de.lise.fluxflow.mongo.FluxFlowMongoTypeAliases
import de.lise.fluxflow.mongo.MongoDocumentTypePolicy
import de.lise.fluxflow.mongo.MongoTypeAliasIssue
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

    private fun policy(): MongoDocumentTypePolicy {
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
            isRegisteredJvmValueType = ValueTypeConverter(registry)::isRegistered,
        )
    }

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
