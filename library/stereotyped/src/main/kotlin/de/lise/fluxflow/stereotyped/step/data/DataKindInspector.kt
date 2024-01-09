package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.api.step.stateful.data.StepDataKind
import de.lise.fluxflow.stereotyped.step.StepKindInspector
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter

class DataKindInspector private constructor() {
    companion object {
        fun getDataKind(prop: KProperty1<*, *>): DataKind {
            return prop.annotations
                .find { it is Data }
                ?.let { it as Data }?.identifier
                ?.takeUnless { it.isBlank() }
                ?.let { DataKind(it) }
                ?: DataKind(prop.name)
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
    }
}