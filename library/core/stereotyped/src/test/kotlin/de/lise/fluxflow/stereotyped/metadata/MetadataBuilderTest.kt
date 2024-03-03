package de.lise.fluxflow.stereotyped.metadata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@Suppress("unused")
class MetadataBuilderTest {
    annotation class SinglePropAnnotation(
        val singleProp: String
    )

    @SinglePropAnnotation("value")
    class SinglePropTarget

    @Metadata("overwritten")
    annotation class AnnotatedSinglePropAnnotation(
        val singleProp: String
    )

    @AnnotatedSinglePropAnnotation("value")
    class AnnotatedSinglePropAnnotationTarget

    @Test
    fun `metadata keys should use annotation key for single prop annotations`() {
        // Arrange
        val metadataBuilder = MetadataBuilder(mutableMapOf())

        // Act
        val result = metadataBuilder.build(AnnotatedSinglePropAnnotationTarget::class)

        // Assert
        assertThat(result).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "overwritten" to "value"
            )
        )
    }


    annotation class SingleAnnotatedPropAnnotation(
        @get:Metadata("overwrittenProp")
        val singleProp: String
    )

    annotation class NoPropAnnotation

    @NoPropAnnotation
    class NoPropAnnotationTarget

    annotation class MultiPropAnnotation(
        val prop1: String,
        val prop2: Int
    )

    @MultiPropAnnotation("a", 1)
    class MultiPropAnnotationTarget

    @Metadata("overwritten")
    annotation class AnnotatedMultiPropAnnotation(
        val prop1: String,
        val prop2: Int
    )

    @AnnotatedMultiPropAnnotation("a", 1)
    class AnnotatedMultiPropAnnotationTarget

    @Metadata("overwrittenAnnotation")
    annotation class AnnotatedMultiAnnotatedPropAnnotation(
        @get:Metadata("overwrittenProp1")
        val prop1: String,
        @get:Metadata("overwrittenProp2")
        val prop2: Int,
        val prop3: Boolean
    )

    @AnnotatedMultiAnnotatedPropAnnotation("a", 1, true)
    class AnnotatedMultiAnnotatedPropAnnotationTarget

    @Test
    fun `metadata keys should use a concatenation of the annotation's overwritten key and overwritten property key`() {
        // Arrange
        val metadataBuilder = MetadataBuilder(mutableMapOf())

        // Act
        val result = metadataBuilder.build(AnnotatedMultiAnnotatedPropAnnotationTarget::class)

        // Assert
        assertThat(result).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "overwrittenAnnotation-overwrittenProp1" to "a",
                "overwrittenAnnotation-overwrittenProp2" to 1,
            )
        )
    }

    @Test
    fun `annotation params should be ignored if is not annotated with @Metadata while other params are`() {
        // Arrange
        val metadataBuilder = MetadataBuilder(mutableMapOf())

        // Act
        val result = metadataBuilder.build(AnnotatedMultiAnnotatedPropAnnotationTarget::class)

        // Assert
        assertThat(result).doesNotContainKeys("overwrittenAnnotation.prop3")
    }
    
    
    @Metadata("annotationOverwrite")
    annotation class FunctionMetadataAnnotation(
        @get:Metadata("stringOverwrite")
        val stringProperty: String,
        @get:Metadata("intOverwrite")
        val intProperty: Int
    )
    class FunctionMetadataAnnotationTarget {
        @FunctionMetadataAnnotation("test-string", 42)
        fun someRandomFunction() {}
    }
    
    @Test
    fun `metadata applied to functions should also be supported`() {
        // Arrange
        val metadataBuilder = MetadataBuilder(mutableMapOf())
        
        // Act
        val result = metadataBuilder.build(FunctionMetadataAnnotationTarget::someRandomFunction)
        
        // Assert
        assertThat(result).containsExactlyInAnyOrderEntriesOf(mapOf(
            "annotationOverwrite-stringOverwrite" to "test-string",
            "annotationOverwrite-intOverwrite" to 42
        ))
    }
}