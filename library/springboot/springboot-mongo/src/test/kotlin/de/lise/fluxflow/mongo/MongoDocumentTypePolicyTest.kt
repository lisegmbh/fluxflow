package de.lise.fluxflow.mongo

import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.mongo.generic.SimpleType
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.reflection.types.TypeManifestEntry
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.UnknownTypeException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bson.Document
import org.junit.jupiter.api.Test

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
    ): MongoDocumentTypePolicy {
        val registry = TypeRegistry.create(
            javaClass.classLoader,
            listOf(
                entry(TypeRole.MODEL, "model", AllowedModel::class.java),
                entry(TypeRole.VALUE, "value", AllowedValue::class.java),
                entry(TypeRole.VALUE, "value-only", ValueOnly::class.java),
            ),
        )
        val aliases = FluxFlowMongoTypeAliases(
            registry,
            setOf(WorkflowDocument::class.java, StepDocument::class.java),
            setOf(SimpleType::class.java),
        )
        return MongoDocumentTypePolicy(aliases, typeKey, maxDepth, maxNodes)
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
}
