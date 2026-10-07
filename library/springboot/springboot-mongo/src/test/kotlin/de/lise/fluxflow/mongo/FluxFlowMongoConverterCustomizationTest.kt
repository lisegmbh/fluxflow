package de.lise.fluxflow.mongo

import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver
import org.springframework.data.mongodb.core.mapping.MongoMappingContext

class FluxFlowMongoConverterCustomizationTest {
    @Test
    fun `map key replacement is applied to internal converter writes and reads`() {
        val converter = converter()
        FluxFlowMongoConverterCustomization(converter).setMapKeyDotReplacement("~")

        val document = Document()
        converter.write(MapKeyContainer(mapOf("key.with.dot" to "value")), document)

        assertThat(document.get("entries", Document::class.java))
            .containsEntry("key~with~dot", "value")
        assertThat(converter.read(MapKeyContainer::class.java, document))
            .isEqualTo(MapKeyContainer(mapOf("key.with.dot" to "value")))
    }

    @Test
    fun `preserved map keys are written without replacement`() {
        val converter = converter()
        FluxFlowMongoConverterCustomization(converter).preserveMapKeys()

        val document = Document()
        converter.write(MapKeyContainer(mapOf("key.with.dot" to "value")), document)

        assertThat(document.get("entries", Document::class.java))
            .containsEntry("key.with.dot", "value")
    }

    private fun converter(): MappingMongoConverter = MappingMongoConverter(
        NoOpDbRefResolver.INSTANCE,
        MongoMappingContext(),
    ).apply {
        afterPropertiesSet()
    }

    data class MapKeyContainer(val entries: Map<String, String>)
}
