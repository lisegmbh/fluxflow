package de.lise.fluxflow.mongo

import com.sun.management.ThreadMXBean
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.mongo.generic.CollectionType
import de.lise.fluxflow.mongo.generic.NullType
import de.lise.fluxflow.mongo.generic.SimpleType
import de.lise.fluxflow.mongo.generic.record.CollectionTypeRecord
import de.lise.fluxflow.mongo.generic.record.JvmTypeMapping
import de.lise.fluxflow.mongo.generic.record.JvmTypeRecord
import de.lise.fluxflow.mongo.generic.record.NullTypeRecord
import de.lise.fluxflow.mongo.generic.record.TypeName
import de.lise.fluxflow.mongo.generic.record.TypeRecordEntry
import de.lise.fluxflow.mongo.generic.record.TypeReference
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.UnknownTypeException
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.Test
import org.springframework.data.mapping.context.MappingContext
import org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty
import java.lang.management.ManagementFactory
import java.lang.reflect.Proxy

class MongoDocumentTypePolicyCachingTest {
    @Test
    fun `repeated reads inspect each mapped type only once`() {
        val fixture = fixture()
        val inspections = linkedMapOf<Any, Int>()
        @Suppress("UNCHECKED_CAST")
        val observed = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(MappingContext::class.java)) { _, method, arguments ->
            if (method.name == "getRequiredPersistentEntity") {
                val type = requireNotNull(arguments?.firstOrNull())
                inspections[type] = (inspections[type] ?: 0) + 1
            }
            method.invoke(fixture.context, *(arguments ?: emptyArray()))
        } as MappingContext<out MongoPersistentEntity<*>, MongoPersistentProperty>
        val policy = policy(observed)

        repeat(10) { policy.validate(StepDocument::class.java, fixture.source) }

        assertThat(inspections).isNotEmpty()
        inspections.forEach { (type, count) ->
            assertThat(count).describedAs("Metadata inspections for %s across repeated reads", type).isEqualTo(1)
        }
    }

    @Test
    fun `rejected nested aliases retain their mapped diagnostic path`() {
        val fixture = fixture()
        fixture.source.get("data_entries", Document::class.java)
            .get("values", Document::class.java)["payload000"] = Document("_class", "unregistered.Value")

        val failure = runCatching { policy(fixture.context).validate(StepDocument::class.java, fixture.source) }
            .exceptionOrNull()

        assertThat(failure).isInstanceOf(UnknownTypeException::class.java)
        assertThat(requireNotNull(failure).suppressed.map { it.message })
            .contains("Invalid Mongo type at 'data_entries.values.payload000._class'")
    }

    @Test
    fun `diagnostic reports allocation per guarded read without a timing threshold`() {
        val bean = ManagementFactory.getThreadMXBean() as? ThreadMXBean
        if (bean == null || !bean.isThreadAllocatedMemorySupported) {
            println("Mongo policy allocation diagnostic unavailable: thread allocation counter unsupported")
            return
        }
        val previouslyEnabled = bean.isThreadAllocatedMemoryEnabled
        try {
            if (!previouslyEnabled) bean.isThreadAllocatedMemoryEnabled = true
            val fixture = fixture()
            val policy = policy(fixture.context)
            repeat(100) { policy.validate(StepDocument::class.java, fixture.source) }
            val threadId = Thread.currentThread().id
            val allocatedBefore = bean.getThreadAllocatedBytes(threadId)
            val timeBefore = System.nanoTime()
            repeat(200) { policy.validate(StepDocument::class.java, fixture.source) }
            val elapsed = System.nanoTime() - timeBefore
            val allocated = bean.getThreadAllocatedBytes(threadId) - allocatedBefore
            println("Mongo policy diagnostic: payloads=100 warmup=100 reads=200 bytesPerRead=${allocated / 200} nsPerRead=${elapsed / 200}")
        } finally {
            if (!previouslyEnabled) bean.isThreadAllocatedMemoryEnabled = false
        }
    }

    private fun fixture(): Fixture {
        val context = MongoMappingContext().apply {
            setFieldNamingStrategy(SnakeCaseFieldNamingStrategy())
            afterPropertiesSet()
        }
        val converter = MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context).apply { afterPropertiesSet() }
        val values = (0 until 100).associate { "payload%03d".format(it) to "safe" }
        val step = StepDocument("step", "workflow", "kind", "1", values, values, Status.Active)
        val source = Document()
        converter.write(step, source)
        // Use actual converter output and prove it is a valid persisted shape before measuring the policy.
        assertThat(converter.read(StepDocument::class.java, source).toStepData()).isEqualTo(step.toStepData())
        return Fixture(context, source)
    }

    private fun policy(context: MappingContext<out MongoPersistentEntity<*>, MongoPersistentProperty>): MongoDocumentTypePolicy {
        val aliases = FluxFlowMongoTypeAliases(TypeRegistry.create(javaClass.classLoader, emptyList()),
            setOf(StepDocument::class.java),
            setOf(SimpleType::class.java, CollectionType::class.java, NullType::class.java,
                JvmTypeRecord::class.java, CollectionTypeRecord::class.java, NullTypeRecord::class.java,
                TypedRecords::class.java, JvmTypeMapping::class.java, TypeRecordEntry::class.java,
                TypeReference::class.java, TypeName::class.java))
        return MongoDocumentTypePolicy(aliases, fieldNames = MongoFieldNames(context))
    }

    private data class Fixture(val context: MongoMappingContext, val source: Document)
}
