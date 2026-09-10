package de.lise.fluxflow.mongo.security.prototype

import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader
import de.lise.fluxflow.reflection.types.TypeManifestEntry
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.UnknownTypeException
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import java.time.Instant
import java.util.Date

internal const val PROTOTYPE_WITNESS_NAME =
    "de.lise.fluxflow.mongo.security.baseline.fixture.ActivationWitness"

internal const val MODEL_TYPE_ALIAS = "workflow-model"

internal const val VALUE_TYPE_ALIAS = "workflow-value"

internal data class PrototypeWorkflowModel(
    val name: String,
    val nullable: String?,
    val scalar: Int,
    val date: Date,
    val instant: Instant,
    val converted: PrototypeConvertedValue,
    val nested: List<Map<String, Any?>>,
)

internal data class HostOnlyWorkflowModel(
    val value: String,
)

internal data class PrototypeWorkflowValue(
    val value: String,
)

internal data class PrototypeConvertedValue(
    val value: String,
)

@WritingConverter
internal object PrototypeConvertedValueWriter : Converter<PrototypeConvertedValue, String> {
    override fun convert(source: PrototypeConvertedValue): String = "converted:${source.value}"
}

@ReadingConverter
internal object PrototypeConvertedValueReader : Converter<String, PrototypeConvertedValue> {
    override fun convert(source: String): PrototypeConvertedValue {
        require(source.startsWith("converted:"))
        return PrototypeConvertedValue(source.removePrefix("converted:"))
    }
}

internal fun prototypeRegistry(
    loader: WitnessClassLoader,
    vararg entries: TypeManifestEntry,
): TypeRegistry = TypeRegistry.create(loader, entries.toList())

internal fun prototypeEntry(
    role: TypeRole,
    key: String,
    binaryClassName: String,
): TypeManifestEntry = TypeManifestEntry(role, key, binaryClassName, "PR04 contract fixture")

internal fun allowedPrototypeEntries(): Array<TypeManifestEntry> = arrayOf(
    prototypeEntry(
        TypeRole.MODEL,
        PrototypeWorkflowModel::class.java.name,
        PrototypeWorkflowModel::class.java.name,
    ),
    prototypeEntry(
        TypeRole.MODEL,
        MODEL_TYPE_ALIAS,
        PrototypeWorkflowModel::class.java.name,
    ),
    prototypeEntry(
        TypeRole.VALUE,
        PrototypeWorkflowValue::class.java.name,
        PrototypeWorkflowValue::class.java.name,
    ),
    prototypeEntry(
        TypeRole.VALUE,
        VALUE_TYPE_ALIAS,
        PrototypeWorkflowValue::class.java.name,
    ),
)

internal fun prototypeModel(): PrototypeWorkflowModel = PrototypeWorkflowModel(
    name = "contract-model",
    nullable = null,
    scalar = 42,
    date = Date.from(Instant.parse("2026-09-10T08:15:30Z")),
    instant = Instant.parse("2026-09-10T08:15:30Z"),
    converted = PrototypeConvertedValue("custom-conversion"),
    nested = listOf(
        linkedMapOf(
            "null" to null,
            "fqcn" to PrototypeWorkflowValue("legacy-fqcn"),
            "alias" to PrototypeWorkflowValue("logical-alias"),
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
