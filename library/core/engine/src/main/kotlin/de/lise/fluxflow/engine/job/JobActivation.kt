package de.lise.fluxflow.engine.job

import de.lise.fluxflow.api.ioc.IocProvider
import de.lise.fluxflow.api.job.JobDefinition
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.reflection.activation.BasicTypeActivator
import de.lise.fluxflow.reflection.activation.TypeActivator
import de.lise.fluxflow.reflection.activation.function.BasicFunctionResolver
import de.lise.fluxflow.reflection.activation.parameter.FixedValueParameterResolver
import de.lise.fluxflow.reflection.activation.parameter.IocParameterResolver
import de.lise.fluxflow.reflection.activation.parameter.PriorityParameterResolver
import de.lise.fluxflow.reflection.activation.parameter.ValueMatcher
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.UnknownTypeException
import de.lise.fluxflow.stereotyped.job.JobDefinitionBuilder

class JobActivation<TWorkflowModel> @JvmOverloads constructor(
    classLoader: ClassLoader,
    private val jobDefinitionBuilder: JobDefinitionBuilder,
    iocProvider: IocProvider,
    workflow: Workflow<TWorkflowModel>,
    private val jobData: JobData,
    private val typeRegistry: TypeRegistry = TypeRegistry.load(classLoader),
) {

    private val typeActivator: TypeActivator = BasicTypeActivator(
        BasicFunctionResolver(
            PriorityParameterResolver(listOf(
                FixedValueParameterResolver(
                    ValueMatcher.canBeAssigned(),
                    workflow.model
                ),
                PriorityParameterResolver(jobData.parameters.map {
                    FixedValueParameterResolver(
                        ValueMatcher.hasName<Any?>(it.key)
                            .and(ValueMatcher.canBeAssigned()),
                        it.value
                    )
                }),
                IocParameterResolver(iocProvider)
            ))
        )
    )

    fun activate(): JobDefinition {
        val type = try {
            typeRegistry.resolve(TypeRole.JOB, jobData.kind)
        } catch (e: UnknownTypeException) {
            throw JobActivationException(jobData.id, jobData.kind, e)
        }
        val activatedObject = try {
            typeActivator.findActivation(type)?.activate()
        } catch (exception: Exception) {
            throw JobActivationException(jobData.id, jobData.kind, exception)
        } catch (error: LinkageError) {
            throw JobActivationException(jobData.id, jobData.kind, error)
        }
        return when (activatedObject) {
            null ->
                throw JobActivationException(jobData.id, jobData.kind)
            is JobDefinition -> {
                activatedObject
            }
            else -> {
                jobDefinitionBuilder.build(activatedObject)
            }
        }
    }
}
