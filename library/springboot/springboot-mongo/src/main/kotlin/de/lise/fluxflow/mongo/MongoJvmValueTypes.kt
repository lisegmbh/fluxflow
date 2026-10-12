package de.lise.fluxflow.mongo

/** Fixed compatibility types emitted by TypeSpec.fromValue; no package-wide class authorization. */
internal object MongoJvmValueTypes {
    private val primitiveNames = setOf("boolean", "byte", "short", "int", "long", "float", "double", "char")
    private val scalarNames = primitiveNames + setOf(
        "java.lang.String", "java.lang.Boolean", "java.lang.Byte", "java.lang.Short", "java.lang.Integer",
        "java.lang.Long", "java.lang.Float", "java.lang.Double", "java.lang.Character",
        "java.math.BigDecimal", "java.math.BigInteger", "java.util.UUID", "java.util.Date",
        "java.sql.Date", "java.sql.Time", "java.sql.Timestamp",
        "java.time.Instant", "java.time.LocalDate", "java.time.LocalTime", "java.time.LocalDateTime",
        "java.time.OffsetDateTime", "java.time.OffsetTime", "java.time.ZonedDateTime", "java.time.Duration",
        "java.time.Period", "java.time.Year", "java.time.YearMonth", "java.time.MonthDay",
        "org.bson.types.ObjectId", "org.bson.types.Decimal128", "org.bson.types.Binary",
    )
    private val containerNames = setOf(
        "java.util.List", "java.util.Set", "java.util.Map", "java.util.Collection",
        "java.util.ArrayList", "java.util.LinkedList", "java.util.HashSet", "java.util.LinkedHashSet",
        "java.util.TreeSet", "java.util.HashMap", "java.util.LinkedHashMap", "java.util.TreeMap",
        "kotlin.collections.EmptyList", "kotlin.collections.EmptySet", "kotlin.collections.EmptyMap",
    ) + listOf(
        "java.util.Arrays.ArrayList",
        "java.util.Collections.SingletonList", "java.util.Collections.SingletonSet", "java.util.Collections.SingletonMap",
        "java.util.Collections.EmptyList", "java.util.Collections.EmptySet", "java.util.Collections.EmptyMap",
        "java.util.Collections.UnmodifiableCollection", "java.util.Collections.UnmodifiableList",
        "java.util.Collections.UnmodifiableRandomAccessList", "java.util.Collections.UnmodifiableSet",
        "java.util.Collections.UnmodifiableSortedSet", "java.util.Collections.UnmodifiableNavigableSet",
        "java.util.Collections.UnmodifiableMap", "java.util.Collections.UnmodifiableSortedMap",
        "java.util.Collections.UnmodifiableNavigableMap",
        "java.util.ImmutableCollections.List12", "java.util.ImmutableCollections.ListN",
        "java.util.ImmutableCollections.Set12", "java.util.ImmutableCollections.SetN",
        "java.util.ImmutableCollections.Map1", "java.util.ImmutableCollections.MapN",
    ).flatMap { listOf(it, it.substringBeforeLast('.') + "$" + it.substringAfterLast('.')) }

    fun isAllowed(name: String, registeredNames: Set<String>): Boolean {
        if (name in registeredNames) return true
        var end = name.length
        while (end >= 2 && name[end - 2] == '[' && name[end - 1] == ']') end -= 2
        var component = name.substring(0, end)
        if (component.startsWith("[")) {
            component = component.dropWhile { it == '[' }
            if (component in setOf("Z", "B", "S", "I", "J", "F", "D", "C")) return true
            if (!component.startsWith("L") || !component.endsWith(";")) return false
            component = component.substring(1, component.length - 1)
        }
        return component in registeredNames || component in scalarNames || component in containerNames
    }
}
