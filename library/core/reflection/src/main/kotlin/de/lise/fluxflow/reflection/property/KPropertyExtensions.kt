package de.lise.fluxflow.reflection.property

import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaField

inline fun <reified TAnnotation : Annotation> KProperty1<*, *>.findAnnotationEverywhere(): TAnnotation? {
    return this.findAnnotation()
        ?: this.getter.findAnnotation()
        ?: this.javaField?.getAnnotation(TAnnotation::class.java)
}