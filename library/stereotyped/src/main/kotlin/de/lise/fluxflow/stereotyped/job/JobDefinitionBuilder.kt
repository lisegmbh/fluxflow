package de.lise.fluxflow.stereotyped.job

import de.lise.fluxflow.api.job.JobDefinition
import de.lise.fluxflow.api.job.JobKind
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import de.lise.fluxflow.stereotyped.continuation.ContinuationBuilder
import de.lise.fluxflow.stereotyped.job.parameter.ParameterDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.action.ImplicitStatusBehavior
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaMethod

class JobDefinitionBuilder(
    private val parameterDefinitionBuilder: ParameterDefinitionBuilder,
    private val continuationBuilder: ContinuationBuilder,
    private val parameterResolver: ParameterResolver,
    private val cache: MutableMap<KClass<*>, (element: Any) -> JobDefinition>,
) {

    fun <T : Any> build(element: T): JobDefinition {
        val type = element::class

        return cache.getOrPut(type) {
            createBuilder(type)
        }.invoke(element)
    }

    private fun <T: Any> createBuilder(type: KClass<out T>): (element: T) -> JobDefinition {
        val payloadFunction = getPayloadFunction(type)
        val kind = JobKind(
            type.findAnnotation<Job>()?.kind?.takeUnless { it.isBlank() } ?: type.qualifiedName!!
        )

        val parameterBuilders = type
            .memberProperties
            .filter { parameterDefinitionBuilder.isParameterProperty(it) }
            .map { parameterDefinitionBuilder.buildParameterDefinitionFromProperty(it) }
        
        val continuationConverter = continuationBuilder.createResultConverter(
            ImplicitStatusBehavior.Complete, // Jobs are always completed and can not remain in their state
            true,
            payloadFunction
        )

        return  { instance ->
            ReflectedJobDefinition(
                kind,
                parameterBuilders.map { parameterBuilder -> parameterBuilder(instance) },
                emptyMap(), // TODO: Implement metadata construction
                instance
            ) { currentJob, calledInstance ->
                val payloadFunctionResolution = PayloadFunctionResolver(
                    parameterResolver,
                    payloadFunction,
                    { calledInstance },
                    { currentJob }
                ).resolve()

                continuationConverter.toContinuation(
                    payloadFunctionResolution.call()
                )
            }
        }
    }

    private fun <T: Any> getPayloadFunction(type: KClass<out T>): KFunction<*> {
        val possiblePayloadFunctions = type.functions
            .filter { it.visibility == KVisibility.PUBLIC }
            .filter { it.javaMethod?.declaringClass?.let { it != Any::class.java } ?: true }

        val explicitlyDeclaredPayloadFunctions = type.functions
            .filter { it.hasAnnotation<JobPayload>() }

        explicitlyDeclaredPayloadFunctions
            .firstOrNull { !possiblePayloadFunctions.contains(it) }
            ?.let {
                throw JobConfigurationException(
                    "Can not use ${type.qualifiedName}.${it.name} as job payload because it is not accessible."
                )
            }

        if (explicitlyDeclaredPayloadFunctions.size > 1) {
            throw JobConfigurationException(
                "Found multiple explicitly declared payload functions on '${type.qualifiedName}': ${
                    explicitlyDeclaredPayloadFunctions.joinToString(", ") { it.name }
                }"
            )
        }

        val relevantPayloadFunctions = explicitlyDeclaredPayloadFunctions.takeUnless { it.isEmpty() }
            ?: possiblePayloadFunctions

        when(relevantPayloadFunctions.size) {
            0 -> throw JobConfigurationException(
                "Could not find a payload function for job definition '${type.qualifiedName}'."
            )
            1 -> {}
            else -> throw JobConfigurationException(
                "Found multiple functions for type '${type.qualifiedName}' that could be " +
                        "used as a payload function (consider adding @JobPayload annotation). " +
                        "Possible functions are: ${
                            relevantPayloadFunctions.joinToString(", ") { it.name }
                        }"
            )
        }

        return relevantPayloadFunctions.single()
    }
}