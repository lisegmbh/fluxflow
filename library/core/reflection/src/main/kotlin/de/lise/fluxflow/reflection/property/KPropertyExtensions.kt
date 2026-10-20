package de.lise.fluxflow.reflection.property

import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaField

inline fun <reified TAnnotation : Annotation> KProperty<*>.findAnnotationEverywhere(): TAnnotation? {
    return this.findAnnotation()
        ?: this.getter.findAnnotation()
        ?: this.javaField?.getAnnotation(TAnnotation::class.java)
}

fun KProperty<*>.findAnnotationsEverywhere(): List<Annotation> {
    return listOfNotNull(
        this.annotations,
        this.getter.annotations,
        this.javaField?.annotations?.toList()
    ).flatten()
}