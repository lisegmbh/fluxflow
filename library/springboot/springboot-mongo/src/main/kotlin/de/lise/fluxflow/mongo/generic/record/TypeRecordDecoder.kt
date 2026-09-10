package de.lise.fluxflow.mongo.generic.record

import de.lise.fluxflow.mongo.generic.CollectionType
import de.lise.fluxflow.mongo.generic.NullType
import de.lise.fluxflow.mongo.generic.SimpleType
import de.lise.fluxflow.mongo.generic.TypeSpec
import de.lise.fluxflow.mongo.generic.ValueTypeConversionException
import java.util.Collections
import java.util.IdentityHashMap

internal object TypeRecordDecoder {
    fun toTypeSpec(
        record: TypeRecord,
        context: RecordContext,
        maxDepth: Int = 100,
        maxNodes: Int = 100_000,
    ): TypeSpec = Decoder(context, maxDepth, maxNodes).decode(record, "type", 1)

    fun toTypeSpecs(
        records: Map<String, TypeRecord>,
        context: RecordContext,
        maxDepth: Int,
        maxNodes: Int,
    ): Map<String, TypeSpec> {
        val decoder = Decoder(context, maxDepth, maxNodes)
        return records.mapValues { (key, record) ->
            decoder.decode(record, "types[$key]", 1)
        }
    }

    private class Decoder(
        private val context: RecordContext,
        private val maxDepth: Int,
        private val maxNodes: Int,
    ) {
        private val active = Collections.newSetFromMap(IdentityHashMap<TypeRecord, Boolean>())
        private var nodes = 0

        init {
            require(maxDepth > 0) { "Type record maximum depth must be positive" }
            require(maxNodes > 0) { "Type record maximum node count must be positive" }
        }

        fun decode(record: TypeRecord, path: String, depth: Int): TypeSpec {
            nodes++
            if (nodes > maxNodes) {
                throw ValueTypeConversionException(
                    "Type record graph exceeds the maximum node count of $maxNodes at '$path'."
                )
            }
            if (depth > maxDepth) {
                throw ValueTypeConversionException(
                    "Type record graph exceeds the maximum depth of $maxDepth at '$path'."
                )
            }
            if (!active.add(record)) {
                throw ValueTypeConversionException("Cyclic type record graph detected at '$path'.")
            }

            return try {
                when (record) {
                    is NullTypeRecord -> NullType()
                    is JvmTypeRecord -> SimpleType(context.getType(record.jvmTypeReference).value)
                    is CollectionTypeRecord -> CollectionType(
                        decode(record.collectionType, "$path.collectionType", depth + 1),
                        record.componentTypes.mapIndexed { index, component ->
                            decode(component, "$path.componentTypes[$index]", depth + 1)
                        },
                    )
                    else -> throw ValueTypeConversionException(
                        "Unsupported type record '${record::class.java.name}' at '$path'."
                    )
                }
            } finally {
                active.remove(record)
            }
        }
    }
}
