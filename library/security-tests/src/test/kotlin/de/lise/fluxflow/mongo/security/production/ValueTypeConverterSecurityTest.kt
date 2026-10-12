package de.lise.fluxflow.mongo.security.production

import de.lise.fluxflow.mongo.generic.CollectionType
import de.lise.fluxflow.mongo.generic.NullType
import de.lise.fluxflow.mongo.generic.SimpleType
import de.lise.fluxflow.mongo.generic.ValueTypeConversionException
import de.lise.fluxflow.mongo.generic.ValueTypeConverter
import de.lise.fluxflow.mongo.generic.withData
import de.lise.fluxflow.mongo.generic.record.CollectionTypeRecord
import de.lise.fluxflow.mongo.generic.record.JvmTypeMapping
import de.lise.fluxflow.mongo.generic.record.JvmTypeRecord
import de.lise.fluxflow.mongo.generic.record.TypeRecord
import de.lise.fluxflow.mongo.generic.record.TypeRecordEntry
import de.lise.fluxflow.mongo.generic.record.TypeReference
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.reflection.types.TypeManifestEntry
import de.lise.fluxflow.reflection.types.TypeManifestException
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.UnknownTypeException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.context.support.GenericApplicationContext
import java.time.Instant
import java.net.InetAddress
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.util.Collections
import java.util.Date
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ValueTypeConverterSecurityTest {
    @Test
    fun `V04 decoding snapshot is isolated from later source mutations`() {
        val mapping = JvmTypeMapping(mutableListOf(
            TypeRecordEntry(List::class.java.name, "list"),
            TypeRecordEntry(String::class.java.name, "value"),
        ))
        val children = object : AbstractList<TypeRecord>() {
            override val size: Int = 1
            override fun get(index: Int): TypeRecord {
                mapping.entries.clear()
                return JvmTypeRecord(TypeReference("value"))
            }
        }
        val record = CollectionTypeRecord(JvmTypeRecord(TypeReference("list")), children)

        assertThat(record.toTypeSpec(mapping).assertType(listOf("safe"))).isEqualTo(listOf("safe"))
        assertThat(mapping.entries).isEmpty()
    }

    @Test
    fun `V04 snapshots reject identical duplicate references even when unused`() {
        val entry = TypeRecordEntry(String::class.java.name, "duplicate")
        val records = TypedRecords(
            JvmTypeMapping(mutableListOf(entry, entry)),
            emptyMap<String, Any?>(),
            emptyMap(),
        )

        assertThatThrownBy { records.toTypeSafeData() }
            .isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("duplicate")
    }

    @Test
    fun `V04 custom collections are not mutated to reconstruct their elements`() {
        val converter = ValueTypeConverter(registry(
            valueEntry(requireNotNull(TrustedList::class.java.canonicalName), TrustedList::class.java),
        ))
        val date = Date.from(Instant.parse("2026-10-12T08:00:00Z"))
        val custom = TrustedList().apply { add(date) }
        val spec = CollectionType(SimpleType(TrustedList::class), listOf(SimpleType(Instant::class)))

        assertThatThrownBy { converter.assertType(spec, custom) }
            .isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("not compatible")
        assertThat(custom.single()).isSameAs(date)
    }

    @Test
    fun `V04 record decoding reads JVM mappings once despite repeated references`() {
        val entries = CountingEntries(MutableList(1_000) { index ->
            TypeRecordEntry(String::class.java.name, "value-$index")
        }.apply { add(TypeRecordEntry(List::class.java.name, "list")) })
        val record = CollectionTypeRecord(
            JvmTypeRecord(TypeReference("list")),
            List(1_000) { JvmTypeRecord(TypeReference("value-0")) },
        )

        val decoded = record.toTypeSpec(JvmTypeMapping(entries))

        assertThat(decoded.assertType(List(1_000) { "safe" })).isEqualTo(List(1_000) { "safe" })
        assertThat(entries.reads).isLessThanOrEqualTo(2 * entries.size)
    }

    @Test
    fun `V03 typed records retain registered collection instances with nested containers`() {
        val converter = ValueTypeConverter(registry(
            valueEntry(requireNotNull(TrustedList::class.java.canonicalName), TrustedList::class.java),
            valueEntry(requireNotNull(TrustedSet::class.java.canonicalName), TrustedSet::class.java),
        ))
        val customList = TrustedList().apply { add(listOf("safe")); add(setOf("nested")) }
        val customSet = TrustedSet().apply { add(listOf("safe")); add("second") }
        val records = TypedRecords.fromData(mapOf("list" to customList, "set" to customSet))

        val restored = records.toTypeSafeData(converter)

        assertThat(restored["list"]).isSameAs(customList)
        assertThat(restored["set"]).isSameAs(customSet)
        assertThat(customList).containsExactly(listOf("safe"), setOf("nested"))
        assertThat(customSet).containsExactly(listOf("safe"), "second")
    }

    @Test
    fun `V04 nested URLs and existing URL sets cannot bypass hash safety`() {
        var resolutions = 0
        val handler = object : URLStreamHandler() {
            override fun openConnection(url: URL): URLConnection = error("Network access is forbidden")
            override fun getHostAddress(url: URL): InetAddress? {
                resolutions++
                return null
            }
        }
        val url = URL(null, "http://nested.example.invalid/value", handler)
        val converter = ValueTypeConverter.builtInsOnly()
        val identitySet = Collections.newSetFromMap(java.util.IdentityHashMap<URL, Boolean>()).apply { add(url) }
        val identityMap = java.util.IdentityHashMap<URL, String>().apply { put(url, "safe") }
        val cases = listOf(
            SimpleType(List::class) to listOf(listOf(url), listOf("safe")),
            SimpleType(Map::class) to listOf(mapOf("url" to url), mapOf("safe" to "value")),
            SimpleType(Map::class) to listOf(identityMap, mapOf("safe" to "value")),
        )
        cases.forEach { (elementType, values) ->
            val spec = CollectionType(SimpleType(Set::class), List(2) { elementType })
            assertThatThrownBy { converter.assertType(spec, values) }
                .isExactlyInstanceOf(ValueTypeConversionException::class.java)
                .hasMessageContaining("URL")
            assertThat(resolutions).isZero()
        }
        assertThatThrownBy { converter.assertType(SimpleType(Set::class), identitySet) }
            .isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("URL")
        assertThatThrownBy { converter.assertType(SimpleType(Map::class), mapOf("set" to identitySet)) }
            .isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("URL")
        assertThat(resolutions).isZero()
        assertThat(converter.assertType(SimpleType(URL::class), url)).isSameAs(url)
        assertThat(converter.assertType(SimpleType(List::class), listOf(url))).isInstanceOf(List::class.java)
        assertThat(resolutions).isZero()
    }

    @Test
    fun `V04 URL sets are rejected before host resolution`() {
        var resolutions = 0
        val handler = object : URLStreamHandler() {
            override fun openConnection(url: URL): URLConnection = error("Network access is forbidden")
            override fun getHostAddress(url: URL): InetAddress? {
                resolutions++
                return null
            }
        }
        val urls = listOf(
            URL(null, "http://first.example.invalid/value", handler),
            URL(null, "http://second.example.invalid/value", handler),
        )
        val spec = CollectionType(SimpleType(Set::class), List(2) { SimpleType(URL::class) })

        assertThatThrownBy { ValueTypeConverter.builtInsOnly().assertType(spec, urls) }
            .isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("URL")
        assertThat(resolutions).isZero()
    }

    @Test
    fun `V03 registered enum accepts exact key binary and canonical registrations`() {
        val type = TrustedEnum::class.java
        val converter = ValueTypeConverter(
            registry(
                valueEntry("trusted-enum", type),
                valueEntry(type.name, type),
                valueEntry(requireNotNull(type.canonicalName), type),
            )
        )

        assertThat(converter.assertType(SimpleType("trusted-enum"), "first"))
            .isEqualTo(TrustedEnum.FIRST)
        assertThat(converter.assertType(SimpleType(TrustedEnum::class.java.name), "SECOND"))
            .isEqualTo(TrustedEnum.SECOND)
        assertThat(
            converter.assertType(
                SimpleType(requireNotNull(TrustedEnum::class.java.canonicalName)),
                "first",
            )
        ).isEqualTo(TrustedEnum.FIRST)
    }

    @Test
    fun `V03 typed records restore registered enum collections date instant and null`() {
        val converter = converter("trusted-enum", TrustedEnum::class.java)
        val mapping = JvmTypeMapping(
            mutableListOf(
                TypeRecordEntry("trusted-enum", "enum"),
                TypeRecordEntry(
                    requireNotNull(Collections.singletonList("value")::class.java.canonicalName),
                    "list",
                ),
                TypeRecordEntry(
                    requireNotNull(Collections.singleton("value")::class.java.canonicalName),
                    "set",
                ),
                TypeRecordEntry(Instant::class.java.name, "instant"),
            )
        )
        val instant = Instant.parse("2026-09-10T08:15:30Z")
        val records = TypedRecords(
            mapping,
            linkedMapOf<String, Any?>(
                "list" to listOf("first"),
                "set" to listOf("second"),
                "instant" to Date.from(instant),
                "null" to null,
            ),
            linkedMapOf(
                "list" to CollectionTypeRecord(
                    JvmTypeRecord(TypeReference("list")),
                    listOf(JvmTypeRecord(TypeReference("enum"))),
                ),
                "set" to CollectionTypeRecord(
                    JvmTypeRecord(TypeReference("set")),
                    listOf(JvmTypeRecord(TypeReference("enum"))),
                ),
                "instant" to JvmTypeRecord(TypeReference("instant")),
                "null" to de.lise.fluxflow.mongo.generic.record.NullTypeRecord(),
            ),
        )

        val result = records.toTypeSafeData(converter)

        assertThat(result["list"]).isEqualTo(listOf(TrustedEnum.FIRST))
        assertThat(result["set"]).isEqualTo(setOf(TrustedEnum.SECOND))
        assertThat(result["instant"]).isEqualTo(instant)
        assertThat(result["null"]).isNull()
    }

    @Test
    fun `V03 built in collection variants and null remain available without a registry`() {
        val singletonList = Collections.singletonList("value")
        val singletonSet = Collections.singleton("value")
        val listSpec = CollectionType(
            SimpleType(singletonList::class),
            listOf(SimpleType(String::class)),
        )
        val setSpec = CollectionType(
            SimpleType(singletonSet::class),
            listOf(SimpleType(String::class)),
        )

        assertThat(listSpec.assertType(listOf("value"))).isEqualTo(listOf("value"))
        assertThat(setSpec.assertType(listOf("value"))).isEqualTo(setOf("value"))
        assertThat(
            CollectionType(SimpleType(emptyList<Any>()::class), emptyList())
                .assertType(emptyList<Any>())
        ).isEqualTo(emptyList<Any>())
        assertThat(NullType().assertType(null)).isNull()
    }

    @Test
    fun `V03 legacy no arg conversion is built ins only`() {
        assertThat(SimpleType(String::class).assertType("safe")).isEqualTo("safe")

        assertThatThrownBy {
            SimpleType(TrustedEnum::class).assertType("FIRST")
        }.isExactlyInstanceOf(UnknownTypeException::class.java)
            .hasMessageContaining(requireNotNull(TrustedEnum::class.java.canonicalName))
    }

    @Test
    fun `V04 unknown value type fails with its exact role and key`() {
        val converter = ValueTypeConverter.builtInsOnly()

        val failure = runCatching {
            converter.assertType(SimpleType("unknown.value.Type"), "payload")
        }.exceptionOrNull()

        assertThat(failure).isExactlyInstanceOf(UnknownTypeException::class.java)
        assertThat((failure as UnknownTypeException).role).isEqualTo(TypeRole.VALUE)
        assertThat(failure.key).isEqualTo("unknown.value.Type")
    }

    @Test
    fun `V04 missing and duplicate JVM type references fail deterministically`() {
        val missing = TypedRecords(
            JvmTypeMapping(),
            mapOf("payload" to "safe"),
            mapOf("payload" to JvmTypeRecord(TypeReference("missing"))),
        )
        val duplicate = TypedRecords(
            JvmTypeMapping(
                mutableListOf(
                    TypeRecordEntry(String::class.java.name, "duplicate"),
                    TypeRecordEntry(Int::class.javaObjectType.name, "duplicate"),
                )
            ),
            mapOf("payload" to "safe"),
            mapOf("payload" to JvmTypeRecord(TypeReference("duplicate"))),
        )

        assertThatThrownBy { missing.toTypeSafeData() }
            .isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("missing")
        assertThatThrownBy { duplicate.toTypeSafeData() }
            .isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("duplicate")
    }

    @Test
    fun `V04 record keys and collection lengths must match their values`() {
        val mismatchedKeys = TypedRecords(
            JvmTypeMapping(),
            mapOf("value-only" to "safe"),
            mapOf("type-only" to de.lise.fluxflow.mongo.generic.record.NullTypeRecord()),
        )
        val mismatchedCollection = CollectionType(
            SimpleType(List::class),
            listOf(SimpleType(String::class)),
        )

        assertThatThrownBy { mismatchedKeys.toTypeSafeData() }
            .isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("missing types=[value-only]")
            .hasMessageContaining("missing values=[type-only]")
        assertThatThrownBy {
            mapOf("type-only" to SimpleType(String::class))
                .withData(mapOf("value-only" to "safe"))
        }.isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("missing types=[value-only]")
            .hasMessageContaining("missing values=[type-only]")
        assertThatThrownBy { mismatchedCollection.assertType(emptyList<Any>()) }
            .isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("sizes differ")
    }

    @Test
    fun `V04 cyclic and deep record graphs fail before unbounded recursion`() {
        val mapping = JvmTypeMapping(
            mutableListOf(TypeRecordEntry(List::class.java.name, "list"))
        )
        val children = mutableListOf<TypeRecord>()
        val cycle = CollectionTypeRecord(JvmTypeRecord(TypeReference("list")), children)
        children += cycle
        var deep: TypeRecord = JvmTypeRecord(TypeReference("list"))
        repeat(8) {
            deep = CollectionTypeRecord(JvmTypeRecord(TypeReference("list")), listOf(deep))
        }

        assertThatThrownBy { cycle.toTypeSpec(mapping) }
            .isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("Cyclic")
        assertThatThrownBy {
            ValueTypeConverter(emptyRegistry(), maxDepth = 4).toTypeSafeData(
                TypedRecords(mapping, mapOf("payload" to emptyList<Any>()), mapOf("payload" to deep))
            )
        }.isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("maximum depth")
    }

    @Test
    fun `V04 wide type graphs respect the node limit`() {
        val converter = ValueTypeConverter(emptyRegistry(), maxNodes = 3)
        val spec = CollectionType(
            SimpleType(List::class),
            listOf(
                SimpleType(String::class),
                SimpleType(String::class),
                SimpleType(String::class),
            ),
        )

        assertThatThrownBy { converter.assertType(spec, listOf("a", "b", "c")) }
            .isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("maximum node count")
    }

    @Test
    fun `V04 a logical value key does not authorize implicit class-name keys`() {
        val converter = converter("trusted-enum", TrustedEnum::class.java)

        assertThatThrownBy {
            converter.assertType(SimpleType(TrustedEnum::class.java.name), "FIRST")
        }.isExactlyInstanceOf(UnknownTypeException::class.java)
        assertThatThrownBy {
            converter.assertType(
                SimpleType(requireNotNull(TrustedEnum::class.java.canonicalName)),
                "FIRST",
            )
        }.isExactlyInstanceOf(UnknownTypeException::class.java)
    }

    @Test
    fun `V04 incompatible values and unsupported records never pass through raw`() {
        assertThatThrownBy { SimpleType(Int::class).assertType("not-an-int") }
            .isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("not compatible")
        assertThatThrownBy { NullType().assertType("not-null") }
            .isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("Expected null")

        val unsupported = object : TypeRecord {
            override fun toTypeSpec(context: de.lise.fluxflow.mongo.generic.record.RecordContext) =
                throw AssertionError("Untrusted record implementation must not control decoding")
        }
        val records = TypedRecords(
            JvmTypeMapping(),
            mapOf("payload" to "safe"),
            mapOf("payload" to unsupported),
        )
        assertThatThrownBy { records.toTypeSafeData() }
            .isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("Unsupported type record")
    }

    @Test
    fun `V04 enums nested in untyped maps are rejected instead of losing type information`() {
        val converter = converter("trusted-enum", TrustedEnum::class.java)

        assertThatThrownBy {
            converter.assertType(
                SimpleType(LinkedHashMap::class),
                linkedMapOf("state" to TrustedEnum.FIRST),
            )
        }.isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("Enum values inside maps")
            .hasMessageContaining("value[state]")
    }

    @Test
    fun `V04 untyped map inspection respects cycle depth and node limits`() {
        val mapType = SimpleType(LinkedHashMap::class)
        val cycle = linkedMapOf<String, Any?>()
        cycle["self"] = cycle

        assertThatThrownBy {
            ValueTypeConverter(emptyRegistry()).assertType(mapType, cycle)
        }.isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("Cyclic untyped map value")
        assertThatThrownBy {
            ValueTypeConverter(emptyRegistry(), maxDepth = 2).assertType(
                mapType,
                linkedMapOf("nested" to linkedMapOf("value" to "safe")),
            )
        }.isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("maximum depth")
        assertThatThrownBy {
            ValueTypeConverter(emptyRegistry(), maxNodes = 2).assertType(
                mapType,
                linkedMapOf("first" to "safe", "second" to "safe"),
            )
        }.isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("maximum node count")
    }

    @Test
    fun `V04 registered custom container aliases require actual instances`() {
        val converter = ValueTypeConverter(
            registry(
                valueEntry("custom-map", TrustedMap::class.java),
                valueEntry("custom-list", TrustedList::class.java),
                valueEntry("custom-set", TrustedSet::class.java),
            )
        )

        assertThatThrownBy {
            converter.assertType(SimpleType("custom-map"), linkedMapOf("value" to "safe"))
        }.isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("not compatible")
        assertThatThrownBy {
            converter.assertType(SimpleType("custom-list"), listOf("safe"))
        }.isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("not compatible")
        assertThatThrownBy {
            converter.assertType(SimpleType("custom-set"), setOf("safe"))
        }.isExactlyInstanceOf(ValueTypeConversionException::class.java)
            .hasMessageContaining("not compatible")

        val customMap = TrustedMap().apply { put("value", "safe") }
        val customList = TrustedList().apply { add("safe") }
        val customSet = TrustedSet().apply { add("safe") }
        assertThat(converter.assertType(SimpleType("custom-map"), customMap))
            .isSameAs(customMap)
        assertThat(converter.assertType(SimpleType("custom-list"), customList))
            .isSameAs(customList)
        assertThat(converter.assertType(SimpleType("custom-set"), customSet))
            .isSameAs(customSet)
    }

    @Test
    fun `V04 value aliases conflicting with built ins fail during construction`() {
        val registry = registry(
            TypeManifestEntry(
                TypeRole.VALUE,
                String::class.java.name,
                TrustedEnum::class.java.name,
                "conflict fixture",
            )
        )

        assertThatThrownBy { ValueTypeConverter(registry) }
            .isExactlyInstanceOf(TypeManifestException::class.java)
            .hasMessageContaining(String::class.java.name)
    }

    @Test
    fun `V05 concurrent application contexts keep their value registries isolated`() {
        val firstContext = valueContext("context-enum", FirstContextEnum::class.java)
        val secondContext = valueContext("context-enum", SecondContextEnum::class.java)
        val first = firstContext.getBean(ValueTypeConverter::class.java)
        val second = secondContext.getBean(ValueTypeConverter::class.java)
        val start = CountDownLatch(1)
        val failures = ConcurrentLinkedQueue<Throwable>()
        val executor = Executors.newFixedThreadPool(8)
        val futures = (0 until 8).map { worker ->
            executor.submit {
                start.await()
                repeat(100) {
                    try {
                        val converter = if (worker % 2 == 0) first else second
                        val expected = if (worker % 2 == 0) {
                            FirstContextEnum.ONLY
                        } else {
                            SecondContextEnum.ONLY
                        }
                        assertThat(converter.assertType(SimpleType("context-enum"), "ONLY"))
                            .isEqualTo(expected)
                    } catch (failure: Throwable) {
                        failures += failure
                    }
                }
            }
        }

        try {
            start.countDown()
            futures.forEach { it.get(10, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
            firstContext.close()
            secondContext.close()
        }

        assertThat(failures).isEmpty()
        assertThatThrownBy {
            first.assertType(SimpleType(SecondContextEnum::class), "ONLY")
        }.isExactlyInstanceOf(UnknownTypeException::class.java)
        assertThatThrownBy {
            second.assertType(SimpleType(FirstContextEnum::class), "ONLY")
        }.isExactlyInstanceOf(UnknownTypeException::class.java)
    }

    private fun converter(key: String, type: Class<out Enum<*>>): ValueTypeConverter =
        ValueTypeConverter(
            registry(valueEntry(key, type))
        )

    private fun valueEntry(
        key: String,
        type: Class<*>,
    ): TypeManifestEntry = TypeManifestEntry(TypeRole.VALUE, key, type.name, "value test")

    private fun valueContext(
        key: String,
        type: Class<out Enum<*>>,
    ): GenericApplicationContext = GenericApplicationContext().apply {
        val typeRegistry = registry(
            TypeManifestEntry(TypeRole.VALUE, key, type.name, "context value test")
        )
        beanFactory.registerSingleton("typeRegistry", typeRegistry)
        beanFactory.registerSingleton("valueTypeConverter", ValueTypeConverter(typeRegistry))
        refresh()
    }

    private fun emptyRegistry(): TypeRegistry = registry()

    private fun registry(vararg entries: TypeManifestEntry): TypeRegistry =
        TypeRegistry.create(javaClass.classLoader, entries.toList())

    private class CountingEntries(private val backing: MutableList<TypeRecordEntry>) : AbstractMutableList<TypeRecordEntry>() {
        var reads = 0
            private set
        override val size: Int get() = backing.size
        override fun get(index: Int): TypeRecordEntry = backing[index].also { reads++ }
        override fun add(index: Int, element: TypeRecordEntry) = backing.add(index, element)
        override fun removeAt(index: Int): TypeRecordEntry = backing.removeAt(index)
        override fun set(index: Int, element: TypeRecordEntry): TypeRecordEntry = backing.set(index, element)
    }

    private enum class TrustedEnum { FIRST, SECOND }
    private enum class FirstContextEnum { ONLY }
    private enum class SecondContextEnum { ONLY }
    private class TrustedMap : LinkedHashMap<String, Any?>()
    private class TrustedList : ArrayList<Any?>()
    private class TrustedSet : LinkedHashSet<Any?>()
}
