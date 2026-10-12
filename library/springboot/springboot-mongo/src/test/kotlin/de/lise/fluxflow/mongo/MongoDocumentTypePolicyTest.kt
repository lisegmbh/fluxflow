package de.lise.fluxflow.mongo

import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.mongo.generic.SimpleType
import de.lise.fluxflow.mongo.generic.record.JvmTypeMapping
import de.lise.fluxflow.mongo.generic.record.TypeRecordEntry
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.step.definition.StepDefinitionDocument
import de.lise.fluxflow.mongo.step.definition.DataDefinitionDocument
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.reflection.types.TypeManifestEntry
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.UnknownTypeException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bson.Document
import org.junit.jupiter.api.Test
import org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy
import org.springframework.data.mongodb.core.mapping.MongoMappingContext

class MongoDocumentTypePolicyTest {
    @Test
    fun `D08 rejects malformed and role-invalid aliases`() {
        val policy = policy()

        listOf("unknown", "", 42).forEach { alias ->
            assertThatThrownBy {
                policy.validate(
                    WorkflowDocument::class.java,
                    workflow(Document("_class", alias)),
                )
            }.isInstanceOfAny(UnknownTypeException::class.java, IllegalArgumentException::class.java)
        }

        assertThatThrownBy {
            policy.validate(
                WorkflowDocument::class.java,
                workflow(Document("_class", "value-only")),
            )
        }.isInstanceOf(UnknownTypeException::class.java)
    }

    @Test
    fun `D03 D08 validates model and nested value aliases with a custom type key`() {
        val policy = policy(typeKey = "@type")
        val model = Document("@type", "model")
            .append(
                "payload",
                listOf(Document("nested", Document("@type", "value")))
            )

        policy.validate(WorkflowDocument::class.java, workflow(model, "@type"))

        model["payload"] = listOf(Document("nested", Document("@type", "model")))
        assertThatThrownBy {
            policy.validate(WorkflowDocument::class.java, workflow(model, "@type"))
        }.isInstanceOf(UnknownTypeException::class.java)
    }

    @Test
    fun `D03 D04 infrastructure aliases are limited to framework metadata fields`() {
        val policy = policy()
        fun typeSpec() = Document("_class", SimpleType::class.java.name)
            .append("typeName", String::class.java.name)
        val validStep = Document("_id", "step")
            .append("data", Document())
            .append("dataTypeMap", Document("payload", typeSpec()))
            .append("_class", StepDocument::class.java.name)

        policy.validate(StepDocument::class.java, validStep)

        validStep["data"] = Document("payload", typeSpec())
        assertThatThrownBy {
            policy.validate(StepDocument::class.java, validStep)
        }.isInstanceOf(UnknownTypeException::class.java)
    }

    @Test
    fun `D04 rejects unregistered class names inside legacy SimpleType metadata`() {
        val className = "unregistered.Enum"
        listOf(true, false).forEach { withDiscriminator ->
            val spec = Document("typeName", className)
            if (withDiscriminator) spec["_class"] = SimpleType::class.java.name
            val source = Document("_class", StepDocument::class.java.name)
                .append("dataTypeMap", Document("payload", spec))

            assertThatThrownBy { policy().validate(StepDocument::class.java, source) }
                .isInstanceOf(UnknownTypeException::class.java)
                .hasMessageContaining(className)
        }
    }

    @Test
    fun `D04 rejects unregistered class names inside current JVM type mappings`() {
        val className = "unregistered.Enum"
        val source = Document("_class", StepDocument::class.java.name)
            .append("dataEntries", Document("jvmTypes", Document("entries", listOf(
                Document("reference", "payload-type").append("type", className),
            ))))

        assertThatThrownBy { policy().validate(StepDocument::class.java, source) }
            .isInstanceOf(UnknownTypeException::class.java)
            .hasMessageContaining(className)
    }

    @Test
    fun `D04 mapped JVM metadata names cannot bypass registry validation`() {
        val className = "unregistered.Enum"
        val context = MongoMappingContext().apply {
            setFieldNamingStrategy(SnakeCaseFieldNamingStrategy())
            afterPropertiesSet()
        }
        val policy = policy(fieldNames = MongoFieldNames(context))
        val sources = listOf(
            Document("data_type_map", Document("payload", Document("type_name", className)
                .append("_class", SimpleType::class.java.name))),
            Document("data_entries", Document("jvm_types", Document("entries", listOf(
                Document("reference", "payload-type").append("type", className),
            )))),
        )
        sources.forEach { source ->
            source["_class"] = StepDocument::class.java.name
            assertThatThrownBy { policy.validate(StepDocument::class.java, source) }
                .isInstanceOf(UnknownTypeException::class.java)
                .hasMessageContaining(className)
        }
    }

    @Test
    fun `D04 direct SimpleType reads reject JVM names without a discriminator`() {
        assertThatThrownBy {
            policy().validate(SimpleType::class.java, Document("typeName", "unregistered.Enum"))
        }.isInstanceOf(UnknownTypeException::class.java)
    }

    @Test
    fun `D04 direct JVM mapping reads reject JVM names without a discriminator`() {
        assertThatThrownBy {
            policy().validate(JvmTypeMapping::class.java, Document("entries", listOf(
                Document("reference", "payload-type").append("type", "unregistered.Enum"),
            )))
        }.isInstanceOf(UnknownTypeException::class.java)
    }

    @Test
    fun `D04 direct JVM entry reads reject JVM names without a discriminator`() {
        assertThatThrownBy {
            policy().validate(TypeRecordEntry::class.java,
                Document("reference", "payload-type").append("type", "unregistered.Enum"))
        }.isInstanceOf(UnknownTypeException::class.java)
    }

    @Test
    fun `D04 permitted JVM metadata includes registered nested enums and fixed compatibility types`() {
        val names = listOf(
            RegisteredEnum::class.java.name,
            RegisteredEnum::class.java.canonicalName,
            "java.util.List", "java.util.Set", "java.time.Instant",
            "java.util.Collections.SingletonList", "java.util.Collections\$SingletonSet",
            "java.util.ImmutableCollections.List12", "kotlin.collections.EmptyMap",
            "int[]", "[[I", "java.lang.String[]", "[Ljava.lang.String;",
            "${RegisteredEnum::class.java.canonicalName}[]",
            "[L${RegisteredEnum::class.java.name};",
        )
        val policy = policy()
        names.forEach { name ->
            policy.validate(StepDocument::class.java, Document("_class", StepDocument::class.java.name)
                .append("dataTypeMap", Document("payload", Document("typeName", name)
                    .append("_class", SimpleType::class.java.name)))
                .append("dataEntries", Document("jvmTypes", Document("entries", listOf(
                    Document("reference", "payload-type").append("type", name),
                )))))
        }
    }

    @Test
    fun `D04 legacy user metadata keys named typeName remain legal`() {
        policy().validate(StepDocument::class.java, Document("_class", StepDocument::class.java.name)
            .append("dataTypeMap", Document("typeName", Document("_class", SimpleType::class.java.name)
                .append("typeName", String::class.java.name)))
            .append("metadataTypeMap", Document("typeName", Document("_class", SimpleType::class.java.name)
                .append("typeName", String::class.java.name))))
    }

    @Test
    fun `D04 JVM metadata rejects model-only classes and malformed names`() {
        listOf<Any?>(AllowedModel::class.java.name, "", 42, null, "[Lunregistered.Enum;").forEach { name ->
            assertThatThrownBy {
                policy().validate(StepDocument::class.java, Document("dataTypeMap",
                    Document("payload", Document("typeName", name))))
            }.isInstanceOfAny(UnknownTypeException::class.java, IllegalArgumentException::class.java)
        }
    }

    @Test
    fun `D04 data definition metadata remains a validated VALUE container`() {
        val metadata = Document("_class", TypedRecords::class.java.name)
            .append("values", Document("payload", Document("_class", "value")))
        val dataDefinition = Document("_class", DataDefinitionDocument::class.java.name)
            .append("metadata", metadata)
        val source = Document("_class", StepDefinitionDocument::class.java.name)
            .append("data", listOf(dataDefinition))

        policy().validate(StepDefinitionDocument::class.java, source)

        metadata["values"] = Document("payload", Document("_class", "model"))
        assertThatThrownBy { policy().validate(StepDefinitionDocument::class.java, source) }
            .isInstanceOf(UnknownTypeException::class.java)
    }

    @Test
    fun `D04 nonexistent nested data field does not authorize data definition infrastructure`() {
        val dataDefinition = Document("_class", DataDefinitionDocument::class.java.name)
            .append("data", Document("_class", DataDefinitionDocument::class.java.name))
        val source = Document("_class", StepDefinitionDocument::class.java.name)
            .append("data", listOf(dataDefinition))

        assertThatThrownBy { policy().validate(StepDefinitionDocument::class.java, source) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Mongo type metadata is not allowed")
    }

    @Test
    fun `D06 projection DTOs keep VALUE role validation for nested aliases`() {
        val projection = Document(
            "payload",
            Document("_class", "model"),
        )

        assertThatThrownBy {
            policy().validateProjection(ProjectionDto::class.java, projection)
        }.isInstanceOf(UnknownTypeException::class.java)
    }

    @Test
    fun `D13 shared document identities are validated in every structural policy`() {
        listOf(true, false).forEach { modelFirst ->
            val shared = Document("_class", "model")
            val source = Document("_class", WorkflowDocument::class.java.name)
            if (modelFirst) {
                source["model"] = shared
                source["extra"] = shared
            } else {
                source["extra"] = shared
                source["model"] = shared
            }

            assertThatThrownBy { policy().validate(WorkflowDocument::class.java, source) }
                .describedAs("The shared instance must not authorize a forbidden field (modelFirst=%s)", modelFirst)
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Mongo type metadata is not allowed")
        }
    }

    @Test
    fun `D13 rejects documents beyond the configured depth`() {
        val policy = policy(maxDepth = 3)
        val model = Document("_class", "model")
            .append("first", Document("second", Document("third", "too deep")))

        assertThatThrownBy {
            policy.validate(WorkflowDocument::class.java, workflow(model))
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("maximum depth of 3")
    }

    @Test
    fun `D13 rejects documents beyond the configured node count`() {
        val policy = policy(maxNodes = 4)
        val model = Document("_class", "model")
            .append("values", listOf(1, 2, 3, 4))

        assertThatThrownBy {
            policy.validate(WorkflowDocument::class.java, workflow(model))
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("maximum node count of 4")
    }

    private fun policy(
        typeKey: String = "_class",
        maxDepth: Int = 100,
        maxNodes: Int = 100_000,
        fieldNames: MongoFieldNames? = null,
    ): MongoDocumentTypePolicy {
        val registry = TypeRegistry.create(
            javaClass.classLoader,
            listOf(
                entry(TypeRole.MODEL, "model", AllowedModel::class.java),
                entry(TypeRole.VALUE, "value", AllowedValue::class.java),
                entry(TypeRole.VALUE, "value-only", ValueOnly::class.java),
                entry(TypeRole.VALUE, "registered-enum", RegisteredEnum::class.java),
            ),
        )
        val aliases = FluxFlowMongoTypeAliases(
            registry,
            setOf(WorkflowDocument::class.java, StepDocument::class.java, StepDefinitionDocument::class.java),
            setOf(SimpleType::class.java, JvmTypeMapping::class.java, TypeRecordEntry::class.java,
                TypedRecords::class.java, DataDefinitionDocument::class.java),
        )
        return MongoDocumentTypePolicy(aliases, typeKey, maxDepth, maxNodes, fieldNames)
    }

    private fun workflow(model: Document, typeKey: String = "_class"): Document =
        Document("_id", "workflow")
            .append("model", model)
            .append("modelType", "model")
            .append(typeKey, WorkflowDocument::class.java.name)

    private fun entry(role: TypeRole, key: String, type: Class<*>): TypeManifestEntry =
        TypeManifestEntry(role, key, type.name, "MongoDocumentTypePolicyTest")

    private data class AllowedModel(val payload: Any?)
    private data class AllowedValue(val value: String)
    private data class ValueOnly(val value: String)
    private data class ProjectionDto(val payload: Any?)
    private enum class RegisteredEnum { SAFE }
}
