package de.lise.fluxflow.engine.step.data

import de.lise.fluxflow.reflection.activation.TypeActivator
import de.lise.fluxflow.reflection.activation.parameter.FunctionParameter
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolution
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import de.lise.fluxflow.reflection.activation.parameter.RecursiveParameterResolver
import de.lise.fluxflow.reflection.property.findAnnotationEverywhere
import de.lise.fluxflow.stereotyped.Import
import kotlin.reflect.full.findAnnotation

class ImportedDataResolver(
    private val typeResolverFactory: (
        functionParam: FunctionParameter<*>,
        importAnnotation: Import,
    ) -> TypeActivator,
) : ParameterResolver {
    override fun resolveParameter(functionParam: FunctionParameter<*>): ParameterResolution? {
        val annotation = (
            functionParam.param.findAnnotation<Import>()
                ?: functionParam.matchingProperty?.findAnnotationEverywhere<Import>()
            ) ?: return null

        return RecursiveParameterResolver(
            typeResolverFactory(
                functionParam,
                annotation
            )
        ).resolveParameter(functionParam)
    }
}