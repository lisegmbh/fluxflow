package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.job.continuation.JobContinuation
import de.lise.fluxflow.api.step.stateful.data.DataDefinition
import de.lise.fluxflow.api.step.stateful.data.ModifiableData
import de.lise.fluxflow.api.step.stateful.data.ModificationPolicy
import de.lise.fluxflow.reflection.ReflectionUtils
import de.lise.fluxflow.reflection.property.findAnnotationEverywhere
import de.lise.fluxflow.stereotyped.Import
import de.lise.fluxflow.stereotyped.job.Job
import de.lise.fluxflow.stereotyped.metadata.MetadataBuilder
import de.lise.fluxflow.stereotyped.step.InstanceAccessor
import de.lise.fluxflow.stereotyped.step.data.KindPrefixBuilder.Companion.and
import de.lise.fluxflow.stereotyped.step.data.KindPrefixBuilder.Companion.build
import de.lise.fluxflow.stereotyped.step.data.validation.ValidationBuilder
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * The [DataDefinitionBuilder] can be used to build [DataDefinition] objects by obtaining a type's properties using reflection.
 */
class DataDefinitionBuilder(
    private val dataListenerDefinitionBuilder: DataListenerDefinitionBuilder,
    private val validationBuilder: ValidationBuilder,
    private val metadataBuilder: MetadataBuilder,
) {
    /**
     * Builds all data definitions that can be obtained by introspecting the given [type].
     * @param type The type to get data definitions from.
     * @return A list of all data definitions. If no data definition could be obtained, an empty list is returned.
     */
    fun <TObject : Any> buildDataDefinition(
        type: KClass<out TObject>,
    ):  List<DataDefinition<*>> {
        return buildDataDefinition(
            type,
            null,
            InstanceAccessor.fromStepInstance()
        )
    }

    private fun <TObject : Any> buildDataDefinition(
        type: KClass<TObject>,
        kindPrefixBuilder: KindPrefixBuilder?,
        instanceAccessor: InstanceAccessor<TObject>
    ): List<ReflectedDataDefinition<*,*>> {
        return type.memberProperties
            .flatMap {
                buildDataDefinition(
                    type,
                    it,
                    kindPrefixBuilder,
                    instanceAccessor
                )
            }
    }

    private fun <TObject : Any> buildDataDefinition(
        instanceType: KClass<TObject>,
        prop: KProperty1<TObject, *>,
        kindPrefixBuilder: KindPrefixBuilder?,
        instanceAccessor: InstanceAccessor<TObject>,
    ): List<ReflectedDataDefinition<*,*>> {
        prop.findAnnotationEverywhere<Import>()?.let { importAnnotation ->
            return buildImportedDataDefinition(
                instanceType,
                prop as KProperty1<TObject, Any>,
                kindPrefixBuilder.and(importAnnotation),
                instanceAccessor
            )
        }
        if (!isDataProperty(prop)) {
            return emptyList()
        }
        return listOf(
            buildDataDefinitionFromProperty(
                instanceType,
                prop,
                kindPrefixBuilder,
                instanceAccessor,
            )
        )
    }

    private fun <TParentObject : Any, TProperty : Any> buildImportedDataDefinition(
        parentType: KClass<TParentObject>,
        prop: KProperty1<TParentObject, TProperty>,
        kindPrefixBuilder: KindPrefixBuilder?,
        parentInstanceAccessor: InstanceAccessor<TParentObject>,
    ): List<ReflectedDataDefinition<*,*>> {
        val returnType: KClass<TProperty> = ReflectionUtils.findReturnClass(prop)
        val newInstanceAccessor: InstanceAccessor<TProperty> = parentInstanceAccessor.and {
            val parent: TParentObject = it
            prop.get(parent)
        }
        val importedDataDefinitions = buildDataDefinition(
            returnType,
            kindPrefixBuilder,
            newInstanceAccessor
        )
        
        return importedDataDefinitions.map { importedResult ->
            val listenersFromImportingLocation = dataListenerDefinitionBuilder.build<TParentObject, Any?>(
                importedResult.kind,
                importedResult.type,
                parentType,
                parentInstanceAccessor
            )
            importedResult.withAdditionalListeners(listenersFromImportingLocation)
        }
    }

    /**
     * Introspects the given property and constructs a corresponding [DataDefinition] object.
     * @return A [Data] object backed by the given property.
     * If the property defines a setter, an instance of [ModifiableData] is returned.
     */
    private fun <TObject : Any> buildDataDefinitionFromProperty(
        instanceType: KClass<TObject>,
        prop: KProperty1<TObject, *>,
        kindPrefixBuilder: KindPrefixBuilder?,
        instanceAccessor: InstanceAccessor<TObject>,
    ): ReflectedDataDefinition<*,*> {
        @Suppress("UNCHECKED_CAST")
        return buildDataDefinitionFromTypedProperty(
            instanceType,
            prop as KProperty1<TObject, Any>,
            kindPrefixBuilder,
            instanceAccessor,
        )
    }

    private fun <TObject, TProp : Any> getIsCalculatedValue(
        prop: KProperty1<TObject, TProp>,
    ): Boolean {
        val persistenceType = prop.findAnnotationEverywhere<Data>()?.persistenceType
            ?: PersistenceType.Auto

        when (persistenceType) {
            PersistenceType.Backed -> return false
            PersistenceType.Calculated -> return true
            else -> {}
        }

        // If the property is not stored within a field, 
        // we can assume that it is "calculated" by the getter.
        return prop.javaField == null
    }

    private fun <TObject : Any, TProp : Any> buildDataDefinitionFromTypedProperty(
        instanceType: KClass<TObject>,
        prop: KProperty1<TObject, TProp>,
        kindPrefixBuilder: KindPrefixBuilder?,
        instanceAccessor: InstanceAccessor<TObject>
    ): ReflectedDataDefinition<TObject, TProp?> {
        val kind = kindPrefixBuilder.build(prop)
        val modificationPolicy = prop.findAnnotationEverywhere<Data>()
            ?.modificationPolicy
            ?: ModificationPolicy.InheritSetting

        val valueType = ReflectionUtils.findReturnType(prop)
        val modifiable = prop is KMutableProperty1<TObject, TProp> && prop.setter.visibility == KVisibility.PUBLIC
        val persistenceType = getIsCalculatedValue(prop)
        val dataListenerDefinitions = dataListenerDefinitionBuilder.build<TObject, TProp?>(
            kind,
            valueType,
            instanceType,
            instanceAccessor
        ).toList()
        
        val validations = validationBuilder.buildValidations(
            kind,
            instanceType,
            prop,
        )
        val metadata = metadataBuilder.build(prop)

        if (modifiable) {
            @Suppress("UNCHECKED_CAST")
            val modifiableProperty = prop as KMutableProperty1<TObject, TProp?>
            return ReflectedDataDefinition(
                kind,
                valueType,
                metadata,
                persistenceType,
                dataListenerDefinitions,
                validations,
                modificationPolicy,
                instanceAccessor,
                { instance -> prop.get(instance) },
                { instance, newVal ->
                    modifiableProperty.set(
                        instance,
                        newVal
                    )
                }
            )
        }

        return ReflectedDataDefinition(
            kind,
            valueType,
            metadata,
            persistenceType,
            dataListenerDefinitions,
            validations,
            modificationPolicy,
            instanceAccessor,
            { prop.get(it) }
        )
    }

    /**
     * Checks if the given property should be interpreted as a [DataDefinition].
     */
    internal fun <TObject : Any> isDataProperty(
        prop: KProperty1<out TObject, *>,
    ): Boolean {
        return prop.visibility == KVisibility.PUBLIC &&
                ReflectionUtils.findReturnClass(prop).let { returnType ->
                    !returnType.hasAnnotation<Job>() &&
                            !returnType.isSubclassOf(JobContinuation::class)
                }
    }
}

