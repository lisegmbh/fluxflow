package de.lise.fluxflow.mongo.security.fixtures

import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader
import de.lise.fluxflow.reflection.types.TypeManifestEntry
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.UnknownTypeException
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.springframework.data.annotation.Id
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.mongodb.core.mapping.Document as MongoDocument
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.repository.MongoRepository
import java.time.Instant
import java.util.Date

internal const val SECURITY_WITNESS_NAME =
    "de.lise.fluxflow.mongo.security.baseline.fixture.ActivationWitness"

internal const val MODEL_TYPE_ALIAS = "workflow-model"

internal const val VALUE_TYPE_ALIAS = "workflow-value"

internal const val VALUE_ENUM_ALIAS = "workflow-enum"

internal const val SUBTYPE_ALIAS = "workflow-subtype"

internal const val AUDIT_STEP_KIND = "registered-audit-step"

internal sealed interface SecurityTestWorkflowModelType {
    val name: String
}

internal data class SecurityTestWorkflowModel(
    @field:Field("mapped_name")
    override val name: String,
    val nullable: String?,
    val scalar: Int,
    val date: Date,
    val instant: Instant,
    val converted: SecurityConvertedValue,
    val nested: List<Map<String, Any?>>,
) : SecurityTestWorkflowModelType

internal data class SecurityTestWorkflowSubtype(
    override val name: String,
    val subtypeValue: String,
) : SecurityTestWorkflowModelType

internal object AuditNestedTypes {
    data class Model(
        val value: String,
    )
}

internal class RegisteredAuditStep

internal data class HostOnlyWorkflowModel(
    val value: String,
)

@MongoDocument("security_host_documents")
internal data class SecurityHostDocument(
    @Id val id: String,
    val model: Any?,
)

internal interface SecurityHostRepository : MongoRepository<SecurityHostDocument, String>

internal data class SecurityTestWorkflowValue(
    val value: String,
)

internal enum class SecurityTestWorkflowEnum {
    Ready,
    Done,
}

internal enum class UnregisteredSecurityTestEnum {
    Poison,
}

internal data class SecurityConvertedValue(
    val value: String,
)

@WritingConverter
internal object SecurityConvertedValueWriter : Converter<SecurityConvertedValue, String> {
    override fun convert(source: SecurityConvertedValue): String = "converted:${source.value}"
}

@ReadingConverter
internal object SecurityConvertedValueReader : Converter<String, SecurityConvertedValue> {
    override fun convert(source: String): SecurityConvertedValue {
        require(source.startsWith("converted:"))
        return SecurityConvertedValue(source.removePrefix("converted:"))
    }
}

internal fun securityTestRegistry(
    loader: WitnessClassLoader,
    vararg entries: TypeManifestEntry,
): TypeRegistry = TypeRegistry.create(loader, entries.toList())

internal fun securityTestEntry(
    role: TypeRole,
    key: String,
    binaryClassName: String,
): TypeManifestEntry = TypeManifestEntry(role, key, binaryClassName, "Mongo security contract fixture")

internal fun allowedSecurityTestEntries(): Array<TypeManifestEntry> = arrayOf(
    securityTestEntry(
        TypeRole.MODEL,
        SecurityTestWorkflowModel::class.java.name,
        SecurityTestWorkflowModel::class.java.name,
    ),
    securityTestEntry(
        TypeRole.MODEL,
        MODEL_TYPE_ALIAS,
        SecurityTestWorkflowModel::class.java.name,
    ),
    securityTestEntry(
        TypeRole.VALUE,
        SecurityTestWorkflowValue::class.java.name,
        SecurityTestWorkflowValue::class.java.name,
    ),
    securityTestEntry(
        TypeRole.VALUE,
        VALUE_TYPE_ALIAS,
        SecurityTestWorkflowValue::class.java.name,
    ),
    securityTestEntry(
        TypeRole.VALUE,
        SecurityTestWorkflowEnum::class.java.name,
        SecurityTestWorkflowEnum::class.java.name,
    ),
    securityTestEntry(
        TypeRole.VALUE,
        VALUE_ENUM_ALIAS,
        SecurityTestWorkflowEnum::class.java.name,
    ),
    securityTestEntry(
        TypeRole.MODEL,
        SecurityTestWorkflowSubtype::class.java.name,
        SecurityTestWorkflowSubtype::class.java.name,
    ),
    securityTestEntry(
        TypeRole.MODEL,
        SUBTYPE_ALIAS,
        SecurityTestWorkflowSubtype::class.java.name,
    ),
    securityTestEntry(
        TypeRole.MODEL,
        "nested-audit-model",
        AuditNestedTypes.Model::class.java.name,
    ),
    securityTestEntry(
        TypeRole.STEP,
        AUDIT_STEP_KIND,
        RegisteredAuditStep::class.java.name,
    ),
)

internal fun securityTestModel(): SecurityTestWorkflowModel = SecurityTestWorkflowModel(
    name = "contract-model",
    nullable = null,
    scalar = 42,
    date = Date.from(Instant.parse("2026-09-10T08:15:30Z")),
    instant = Instant.parse("2026-09-10T08:15:30Z"),
    converted = SecurityConvertedValue("custom-conversion"),
    nested = listOf(
        linkedMapOf(
            "null" to null,
            "fqcn" to SecurityTestWorkflowValue("legacy-fqcn"),
            "alias" to SecurityTestWorkflowValue("logical-alias"),
        )
    ),
)

internal fun rawWorkflow(
    id: String,
    typeKey: String,
    modelType: Any?,
    modelFields: Map<String, Any?> = emptyMap(),
): Document = Document("_id", id)
    .append(
        "model",
        Document(modelFields).append(typeKey, modelType),
    )
    .append("modelType", modelType as? String)

internal fun assertUnknownType(
    failure: Throwable,
    role: TypeRole,
    key: String,
) {
    val rejection = generateSequence(failure as Throwable?) { it.cause }
        .filterIsInstance<UnknownTypeException>()
        .singleOrNull()

    assertThat(rejection)
        .describedAs("The read must fail through the trusted type registry")
        .isNotNull
    assertThat(rejection!!.role).isEqualTo(role)
    assertThat(rejection.key).isEqualTo(key)
}
