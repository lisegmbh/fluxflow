package de.lise.fluxflow.stereotyped.metadata

import de.lise.fluxflow.reflection.property.findAnnotationEverywhere
import de.lise.fluxflow.stereotyped.step.action.ActionDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.automation.AutomationDefinitionBuilder
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties

/**
 * The [MetadataBuilder] can be used to obtain metadata for various workflow elements by reflecting their annotations.
 */
class MetadataBuilder(
    private val typeMetadataCache: MutableMap<KClass<*>, Map<String, Any>> = mutableMapOf(),
    private val functionMetadataCache: MutableMap<KFunction<*>, Map<String, Any>> = mutableMapOf()
) {
    /**
     * Builds the metadata map based on the annotations that are present on the supplied [type].
     * @param type The class representing the type for which the annotation map should be build.
     * @return A non-null map, containing only non-null values.
     * If there is no metadata present for the supplied [type], an empty map is returned.
     */
    fun <T : Any> build(type: KClass<T>): Map<String, Any> {
        return typeMetadataCache.getOrPut(type) {
            createMetadata(type)
        }
    }

    /**
     * Builds the metadata map based on the annotations that are present on the supplied [function].
     * @param function The function for which the annotation map should be built.
     * @return A non-null map, containing only non-null values.
     * If there is no metadata present for the supplied [function], an empty map is returned.
     */
    fun build(function: KFunction<*>): Map<String, Any> {
        return functionMetadataCache.getOrPut(function) {
            createMetadata(function)
        }
    }
    
    private fun createMetadata(function: KFunction<*>): Map<String, Any> {
        return function.annotations
            .filter { 
                !AutomationDefinitionBuilder.isAutomationAnnotation(it)
                        && !ActionDefinitionBuilder.isActionAnnotation(it)
            }
            .flatMap { toMetadata(it).entries }
            .associate { it.key to it.value }
    }


    private fun isIgnoredAnnotation(annotation: Annotation): Boolean {
        return !annotation.annotationClass.hasAnnotation<Metadata>()
    }


    private fun <T : Any> createMetadata(type: KClass<T>): Map<String, Any> {
        return type.annotations
            .filter {
                // The Step annotation should not be treated as metadata
                !isIgnoredAnnotation(it)
            }
            .flatMap { toMetadata(it).entries }
            .associate { it.key to it.value }
    }


    private fun <TAnnotation : Annotation> toMetadata(annotation: TAnnotation): Map<String, Any> {
        val annotationType = annotation.annotationClass
        val memberProperties = annotationType.memberProperties

        val explicitAnnotationTypeKey = annotationType.findAnnotation<Metadata>()?.key
        val implicitAnnotationTypeKey = lowercaseStart(annotationType.simpleName!!)

        if (memberProperties.isEmpty()) {
            return mapOf<String, Any>(keyFromAnnotation(explicitAnnotationTypeKey, implicitAnnotationTypeKey) to true)
        }

        val hasExplicitlyAnnotatedProperty = memberProperties.any { it.findAnnotationEverywhere<Metadata>() != null }

        val relevantProperties = memberProperties.filter {
            // If at least one property is annotated, only annotated properties will be transformed into metadata
            !hasExplicitlyAnnotatedProperty || it.findAnnotationEverywhere<Metadata>() != null
        }.map {
            @Suppress("UNCHECKED_CAST")
            it as KProperty1<TAnnotation, Any>
        }

        return relevantProperties.associate {
            keyFromProperty(
                explicitAnnotationTypeKey,
                implicitAnnotationTypeKey,
                it,
                relevantProperties.size == 1
            ) to it.get(annotation)
        }
    }

    private fun keyFromAnnotation(explicitAnnotationTypeKey: String?, implicitAnnotationTypeKey: String): String {
        return explicitAnnotationTypeKey ?: implicitAnnotationTypeKey
    }

    private fun <TAnnotation : Annotation> keyFromProperty(
        explicitAnnotationTypeKey: String?,
        implicitAnnotationTypeKey: String,
        property: KProperty1<out TAnnotation, *>,
        isSingle: Boolean
    ): String {
        val annotation = property.findAnnotationEverywhere<Metadata>()
        val explicitPropertyTypeKey = annotation?.key

        if(isSingle) {
            return explicitPropertyTypeKey ?: explicitAnnotationTypeKey ?: implicitAnnotationTypeKey
        }

        return if (explicitPropertyTypeKey != null) {
            // If the property is annotated
            if (explicitAnnotationTypeKey != null) {
                // The annotation itself as well as the property is annotated -> annotationKey.propertyKey
                "${explicitAnnotationTypeKey}-${explicitPropertyTypeKey}"
            } else {
                // Only the property is annotated -> propertyKey
                explicitPropertyTypeKey
            }
        } else {
            // If the property is not annotated
            val implicitPropertyKey = lowercaseStart(property.name)
            val annotationTypeKey = explicitAnnotationTypeKey ?: implicitAnnotationTypeKey
            "${annotationTypeKey}-${implicitPropertyKey}"
        }
    }

    private fun lowercaseStart(value: String): String {
        return value.substring(0, 1).lowercase() + value.substring(1)
    }
}