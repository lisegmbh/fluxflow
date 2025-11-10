package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.api.step.stateful.data.StepDataKind
import de.lise.fluxflow.reflection.property.findAnnotationEverywhere
import de.lise.fluxflow.stereotyped.Import
import de.lise.fluxflow.stereotyped.step.StepKindInspector
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter

class DataKindInspector private constructor() {
    companion object {
        fun getDataKind(prop: KProperty1<*, *>): DataKind {
            return getDataKind(null, prop)
        }

        fun getStepDataKind(prop: KProperty1<*, *>): StepDataKind {
            return StepDataKind(
                StepKindInspector.getStepKind(
                    prop.javaGetter?.declaringClass?.kotlin
                        ?: prop.javaField?.declaringClass?.kotlin
                        ?: throw IllegalArgumentException("Can not find declaring type for property $prop")
                ),
                getDataKind(prop)
            )
        }
        
        fun <TImportingProperty> getDataKind(
            importingProperty: KProperty1<*, TImportingProperty>,
            importedProperty: KProperty1<TImportingProperty, *>
        ): DataKind {
            return getDataKind(
                importingProperty.findAnnotationEverywhere<Import>(),
                importedProperty
            )
        }
        
        fun getDataKind(
            importAnnotation: Import?,
            prop: KProperty1<*,*>
        ): DataKind {
            val rawKind = prop.annotations
                .find { it is Data }
                ?.let { it as Data }?.identifier
                ?.takeUnless { it.isBlank() }
                ?: prop.name
            
            val finalKind = importAnnotation?.prefixStrategy?.apply(
                importAnnotation.prefix,
                rawKind
            ) ?: rawKind
            
            return DataKind(finalKind)
        }
    }
}