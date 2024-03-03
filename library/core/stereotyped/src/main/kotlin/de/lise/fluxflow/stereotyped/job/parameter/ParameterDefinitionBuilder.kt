package de.lise.fluxflow.stereotyped.job.parameter

import de.lise.fluxflow.api.job.parameter.ParameterDefinition
import de.lise.fluxflow.api.job.parameter.ParameterKind
import de.lise.fluxflow.reflection.ReflectionUtils
import de.lise.fluxflow.stereotyped.job.Job
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.hasAnnotation

class ParameterDefinitionBuilder {

    fun <TObject : Any> isParameterProperty(
        prop: KProperty1<out TObject, *>
    ): Boolean {
        return prop.visibility == KVisibility.PUBLIC &&
                !ReflectionUtils.findReturnClass(prop).hasAnnotation<Job>()
    }

    fun <TObject : Any> buildParameterDefinitionFromProperty(
        prop: KProperty1<out TObject, *>
    ): (instance: TObject) -> ParameterDefinition<*> {
        @Suppress("UNCHECKED_CAST")
        return buildParameterDefinitionFromTypedProperty(
            prop as KProperty1<TObject, Any>
        )
    }

    private fun <TObject, TProp : Any> buildParameterDefinitionFromTypedProperty(
        prop: KProperty1<TObject, TProp>
    ): (element: TObject) -> ParameterDefinition<TProp?> {
        val kind = ParameterKind(prop.name)
        val type = ReflectionUtils.findReturnClass(prop)

        return { instance ->
            ReflectedParameterDefinition(
                kind,
                type.java,
                instance
            ) { prop.get(it) }
        }
    }

}