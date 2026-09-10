package de.lise.fluxflow.mongo.generic

import de.lise.fluxflow.mongo.generic.record.TypeRecordDecoder
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.reflection.types.TypeManifestException
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.UnknownTypeException
import org.bson.Document
import org.bson.types.Binary
import org.bson.types.Decimal128
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Period
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Reconstructs values using an immutable, exact snapshot of trusted value types.
 *
 * Persisted type names are never passed to a class loader. Application types must be registered
 * for the [TypeRole.VALUE] role. Common scalar and collection types are provided by a fixed table.
 */
class ValueTypeConverter private constructor(
    private val aliases: Map<String, KClass<*>>,
    private val normalizableContainerAliases: Set<String>,
    private val maxDepth: Int = DEFAULT_MAX_DEPTH,
    private val maxNodes: Int = DEFAULT_MAX_NODES,
) {
    constructor(
        registry: TypeRegistry,
        maxDepth: Int = DEFAULT_MAX_DEPTH,
        maxNodes: Int = DEFAULT_MAX_NODES,
    ) : this(buildAliases(registry), BuiltInContainerAliases, maxDepth, maxNodes)

    init {
        require(maxDepth > 0) { "Value type maximum depth must be positive" }
        require(maxNodes > 0) { "Value type maximum node count must be positive" }
    }

    fun assertType(spec: TypeSpec, value: Any?): Any? =
        Traversal().assertType(spec, value, "value", 1)

    fun <TValue> toTypeSafeData(records: TypedRecords<TValue>): Map<String, TValue> {
        requireMatchingKeys(records.values, records.types)
        val specs = TypeRecordDecoder.toTypeSpecs(
            records.types,
            records.jvmTypes.snapshot(),
            maxDepth,
            maxNodes,
        )
        return toTypeSafeData(specs, records.values)
    }

    fun <TValue> toTypeSafeData(
        specs: Map<String, TypeSpec>,
        values: Map<String, TValue>,
    ): Map<String, TValue> {
        requireMatchingKeys(values, specs)
        val traversal = Traversal()
        return specs.mapValues { (key, spec) ->
            @Suppress("UNCHECKED_CAST")
            traversal.assertType(spec, values[key], "values[$key]", 1) as TValue
        }
    }

    /** Checks the immutable alias table without resolving or loading a class. */
    internal fun isRegistered(typeName: Any?): Boolean =
        typeName is String && typeName.isNotEmpty() && aliases.containsKey(typeName)

    /** Checks only the fixed, role-independent aliases supported without an application registry. */
    internal fun isBuiltIn(typeName: Any?): Boolean = BuiltInsOnly.isRegistered(typeName)

    private fun resolve(typeName: String): KClass<*> =
        aliases[typeName] ?: throw UnknownTypeException(TypeRole.VALUE, typeName)

    private fun requireMatchingKeys(values: Map<String, *>, specs: Map<String, *>) {
        if (values.keys != specs.keys) {
            val missingTypes = values.keys - specs.keys
            val missingValues = specs.keys - values.keys
            throw ValueTypeConversionException(
                "Value type metadata keys do not match value keys: " +
                        "missing types=$missingTypes, missing values=$missingValues."
            )
        }
    }

    private inner class Traversal {
        private val active = java.util.Collections.newSetFromMap(
            java.util.IdentityHashMap<TypeSpec, Boolean>()
        )
        private val activeUntypedContainers = java.util.Collections.newSetFromMap(
            java.util.IdentityHashMap<Any, Boolean>()
        )
        private var nodes = 0

        fun assertType(spec: TypeSpec, value: Any?, path: String, depth: Int): Any? {
            visitNode(path, depth)
            if (!active.add(spec)) {
                throw ValueTypeConversionException("Cyclic value type graph detected at '$path'.")
            }

            return try {
                when (spec) {
                    is NullType -> assertNull(value, path)
                    is SimpleType -> assertSimple(spec, value, path, depth)
                    is CollectionType -> assertCollection(spec, value, path, depth)
                    else -> throw ValueTypeConversionException(
                        "Unsupported value type specification '${spec::class.java.name}' at '$path'."
                    )
                }
            } finally {
                active.remove(spec)
            }
        }

        private fun assertNull(value: Any?, path: String): Any? {
            if (value != null) {
                throw ValueTypeConversionException("Expected null at '$path'.")
            }
            return null
        }

        private fun assertSimple(
            spec: SimpleType,
            value: Any?,
            path: String,
            depth: Int,
        ): Any? {
            val type = resolve(spec.typeName)
            if (type.isInstance(value)) {
                if (value is Map<*, *>) {
                    assertUntypedMap(value, path, depth)
                }
                return value
            }

            if (spec.typeName in normalizableContainerAliases && value is Collection<*>) {
                if (Set::class.java.isAssignableFrom(type.java)) {
                    return value.toSet()
                }
                if (List::class.java.isAssignableFrom(type.java) ||
                    Collection::class.java == type.java
                ) {
                    return value.toList()
                }
            }

            if (spec.typeName in normalizableContainerAliases &&
                value is Map<*, *> &&
                Map::class.java.isAssignableFrom(type.java)
            ) {
                assertUntypedMap(value, path, depth)
                return value
            }

            if (value is Date && type == Instant::class) {
                return value.toInstant()
            }
            if (value is String && type == Char::class && value.length == 1) {
                return value.single()
            }
            if (value is String && type.java.isEnum) {
                @Suppress("UNCHECKED_CAST")
                val constants = type.java.enumConstants as Array<out Enum<*>>
                return constants.firstOrNull { it.name.equals(value, ignoreCase = true) }
                    ?: throw ValueTypeConversionException(
                        "Value '$value' is not a constant of '${type.java.name}' at '$path'."
                    )
            }

            throw ValueTypeConversionException(
                "Value at '$path' is not compatible with registered type '${spec.typeName}'."
            )
        }

        private fun assertUntypedMap(value: Map<*, *>, path: String, depth: Int) {
            withUntypedContainer(value, path) {
                value.entries.forEachIndexed { index, (key, nestedValue) ->
                    val keyPath = if (key is String) "$path[$key]" else "$path[$index]"
                    inspectUntypedValue(nestedValue, keyPath, depth + 1)
                }
            }
        }

        private fun inspectUntypedValue(value: Any?, path: String, depth: Int) {
            visitNode(path, depth)
            when (value) {
                is Enum<*> -> throw ValueTypeConversionException(
                    "Enum values inside maps require explicit type metadata at '$path'."
                )
                is Map<*, *> -> assertUntypedMap(value, path, depth)
                is Collection<*> -> withUntypedContainer(value, path) {
                    value.forEachIndexed { index, element ->
                        inspectUntypedValue(element, "$path[$index]", depth + 1)
                    }
                }
                is Array<*> -> withUntypedContainer(value, path) {
                    value.forEachIndexed { index, element ->
                        inspectUntypedValue(element, "$path[$index]", depth + 1)
                    }
                }
            }
        }

        private inline fun withUntypedContainer(
            value: Any,
            path: String,
            inspect: () -> Unit,
        ) {
            if (!activeUntypedContainers.add(value)) {
                throw ValueTypeConversionException(
                    "Cyclic untyped map value detected at '$path'."
                )
            }
            try {
                inspect()
            } finally {
                activeUntypedContainers.remove(value)
            }
        }

        private fun visitNode(path: String, depth: Int) {
            nodes++
            if (nodes > maxNodes) {
                throw ValueTypeConversionException(
                    "Value type graph exceeds the maximum node count of $maxNodes at '$path'."
                )
            }
            if (depth > maxDepth) {
                throw ValueTypeConversionException(
                    "Value type graph exceeds the maximum depth of $maxDepth at '$path'."
                )
            }
        }

        private fun assertCollection(
            spec: CollectionType,
            value: Any?,
            path: String,
            depth: Int,
        ): Any? {
            val collection = value as? Collection<*>
                ?: throw ValueTypeConversionException("Expected a collection at '$path'.")
            if (collection.size != spec.componentTypes.size) {
                throw ValueTypeConversionException(
                    "Collection value and type metadata sizes differ at '$path': " +
                            "${collection.size} values, ${spec.componentTypes.size} types."
                )
            }
            val elements = collection.mapIndexed { index, element ->
                assertType(
                    spec.componentTypes[index],
                    element,
                    "$path[$index]",
                    depth + 1,
                )
            }
            return assertType(spec.collectionType, elements, "$path.collection", depth + 1)
        }
    }

    private data class AliasRegistration(
        val alias: String,
        val type: KClass<*>,
    )

    companion object {
        private const val DEFAULT_MAX_DEPTH = 100
        private const val DEFAULT_MAX_NODES = 100_000

        private val BuiltInRegistrations = builtInRegistrations()
        private val BuiltInContainerAliases = BuiltInRegistrations
            .filter { registration ->
                registration.type == List::class ||
                        registration.type == Set::class ||
                        registration.type == Map::class
            }
            .mapTo(mutableSetOf(), AliasRegistration::alias)

        internal val BuiltInsOnly = ValueTypeConverter(
            buildAliasMap(BuiltInRegistrations),
            BuiltInContainerAliases,
            DEFAULT_MAX_DEPTH,
            DEFAULT_MAX_NODES,
        )

        /** Returns the immutable converter used by legacy constructors without a registry. */
        fun builtInsOnly(): ValueTypeConverter = BuiltInsOnly

        private fun buildAliases(registry: TypeRegistry): Map<String, KClass<*>> {
            val registryTypes = registry.entries
                .filter { it.role == TypeRole.VALUE }
                .map { entry ->
                    AliasRegistration(entry.key, entry.type)
                }
            return buildAliasMap(BuiltInRegistrations + registryTypes)
        }

        private fun buildAliasMap(registrations: List<AliasRegistration>): Map<String, KClass<*>> =
            registrations
                .groupBy(AliasRegistration::alias)
                .mapValues { (alias, matches) ->
                    val types = matches.map(AliasRegistration::type).distinct()
                    if (types.size != 1) {
                        throw TypeManifestException(
                            "Value type alias '$alias' resolves to multiple classes: " +
                                    types.joinToString { it.java.name }
                        )
                    }
                    types.single()
                }
                .toMap()

        private fun builtInRegistrations(): List<AliasRegistration> {
            val registrations = mutableListOf<AliasRegistration>()
            fun register(type: KClass<*>, vararg extraAliases: String) {
                val aliases = buildSet {
                    add(type.java.name)
                    type.java.canonicalName?.let(::add)
                    type.qualifiedName?.let(::add)
                    addAll(extraAliases)
                }
                registrations += aliases.map { AliasRegistration(it, type) }
            }

            register(Boolean::class, "boolean", "java.lang.Boolean")
            register(Byte::class, "byte", "java.lang.Byte")
            register(Short::class, "short", "java.lang.Short")
            register(Int::class, "int", "java.lang.Integer")
            register(Long::class, "long", "java.lang.Long")
            register(Float::class, "float", "java.lang.Float")
            register(Double::class, "double", "java.lang.Double")
            register(Char::class, "char", "java.lang.Character")
            register(String::class)
            register(Unit::class)
            register(BigInteger::class)
            register(BigDecimal::class)
            register(UUID::class)
            register(Date::class)
            register(Instant::class)
            register(LocalDate::class)
            register(LocalDateTime::class)
            register(LocalTime::class)
            register(OffsetDateTime::class)
            register(OffsetTime::class)
            register(ZonedDateTime::class)
            register(Duration::class)
            register(Period::class)
            register(Year::class)
            register(YearMonth::class)
            register(ZoneId::class)
            register(Locale::class)
            register(URI::class)
            register(URL::class)
            register(ObjectId::class)
            register(Decimal128::class)
            register(Binary::class)

            register(
                List::class,
                "java.util.Collection",
                "kotlin.collections.Collection",
                "java.util.List",
                "kotlin.collections.List",
                "java.util.ArrayList",
                "java.util.LinkedList",
                "java.util.Vector",
                "java.util.Stack",
                "java.util.Arrays.ArrayList",
                "java.util.Collections.EmptyList",
                "java.util.Collections.SingletonList",
                "java.util.Collections.UnmodifiableList",
                "java.util.Collections.UnmodifiableRandomAccessList",
                "java.util.Collections.SynchronizedList",
                "java.util.Collections.SynchronizedRandomAccessList",
                "java.util.Collections.CheckedList",
                "java.util.Collections.CheckedRandomAccessList",
                "java.util.ImmutableCollections.List12",
                "java.util.ImmutableCollections.ListN",
                "kotlin.collections.EmptyList",
            )
            register(
                Set::class,
                "java.util.Set",
                "kotlin.collections.Set",
                "java.util.HashSet",
                "java.util.LinkedHashSet",
                "java.util.TreeSet",
                "java.util.Collections.EmptySet",
                "java.util.Collections.SingletonSet",
                "java.util.Collections.UnmodifiableSet",
                "java.util.Collections.SynchronizedSet",
                "java.util.Collections.CheckedSet",
                "java.util.ImmutableCollections.Set12",
                "java.util.ImmutableCollections.SetN",
                "kotlin.collections.EmptySet",
            )
            register(
                Map::class,
                "java.util.Map",
                "kotlin.collections.Map",
                "java.util.HashMap",
                "java.util.LinkedHashMap",
                "java.util.TreeMap",
                "java.util.Collections.EmptyMap",
                "java.util.Collections.SingletonMap",
                "java.util.Collections.UnmodifiableMap",
                "java.util.Collections.SynchronizedMap",
                "java.util.Collections.CheckedMap",
                "java.util.ImmutableCollections.Map1",
                "java.util.ImmutableCollections.MapN",
                "kotlin.collections.EmptyMap",
            )
            register(Document::class)

            return registrations
        }
    }
}
