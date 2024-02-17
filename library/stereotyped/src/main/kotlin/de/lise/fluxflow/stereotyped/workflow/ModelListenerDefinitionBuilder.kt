package de.lise.fluxflow.stereotyped.workflow

import de.lise.fluxflow.api.workflow.ModelListenerDefinition
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import de.lise.fluxflow.reflection.isInvokableInstanceFunction
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.functions

class ModelListenerDefinitionBuilder(
    private val parameterResolver: ParameterResolver,
    private val cache: MutableMap<KClass<*>, Set<CachedModelListenerDefinitionBuilder<*>>> = ConcurrentHashMap()
) {
    fun <TModel : Any> build(
        model: TModel?
    ): List<ModelListenerDefinition<TModel>> {
        if(model == null) {
            return emptyList()
        }
        @Suppress("UNCHECKED_CAST")
        val modelClass = model::class as KClass<TModel>

        val builders = cache.computeIfAbsent(modelClass) {
            val computedBuilders = doBuild<TModel, TModel>(
                modelClass,
            )
            computedBuilders
        }

        val cachedBuilders = builders.map {
            @Suppress("UNCHECKED_CAST")
            it as CachedModelListenerDefinitionBuilder<TModel>
        }.toSet()

        return cachedBuilders.map { cachedBuilder ->
            cachedBuilder(model)
        }
    }

    private fun <TFunctionOwner : Any, TModel> doBuild(
        instanceType: KClass<TFunctionOwner>,
    ): Set<(TFunctionOwner) -> ModelListenerDefinition<TModel>> {
        return instanceType.functions
            .mapNotNull {
                doBuild<TFunctionOwner, TModel>(
                    instanceType,
                    it
                )
            }.toSet()
    }

    private fun <TFunctionOwner : Any, TModel> doBuild(
        modelType: KClass<TFunctionOwner>,
        function: KFunction<*>
    ): ((TFunctionOwner) -> ModelListenerDefinition<TModel>)? {
        if (!function.isInvokableInstanceFunction()) {
            return null
        }

        val listenerAnnotations = function.findAnnotations<ModelListener>()
        if (listenerAnnotations.isEmpty()) {
            return null
        }

        return { obj ->
            ReflectedModelListenerDefinition(
                obj
            ) { workflow, instance, old, new ->
                val callable = ModelListenerFunctionResolver(
                    parameterResolver,
                    function,
                    modelType,
                    { workflow },
                    { instance },
                    { old },
                    { new }
                ).resolve()

                callable.call()
            }
        }
    }
}