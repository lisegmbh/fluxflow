package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.ioc.IocProvider
import de.lise.fluxflow.api.step.stateful.StepActivationException
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.engine.step.data.ImportedDataResolver
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.reflection.activation.BasicTypeActivator
import de.lise.fluxflow.reflection.activation.TypeActivator
import de.lise.fluxflow.reflection.activation.function.BasicFunctionResolver
import de.lise.fluxflow.reflection.activation.parameter.FixedValueParameterResolver
import de.lise.fluxflow.reflection.activation.parameter.IocParameterResolver
import de.lise.fluxflow.reflection.activation.parameter.PriorityParameterResolver
import de.lise.fluxflow.reflection.activation.parameter.ValueMatcher
import de.lise.fluxflow.stereotyped.Import
import kotlin.reflect.KClass

class StepSpecificInstanceActivation<TWorkflowModel>(
    iocProvider: IocProvider,
    workflow: Workflow<TWorkflowModel>,
    private val stepData: StepData,
) {
    private val typeActivator = createTypeActivator(
        iocProvider,
        workflow,
        stepData
    )

    fun <TInstance : Any> activateInstance(
        type: KClass<TInstance>,
    ): TInstance {
        return typeActivator.findActivation(type)?.activate()
            ?: throw StepActivationException(
                stepData.id,
                stepData.kind
            )
    }

    private companion object {
        fun <TWorkflowModel> createTypeActivator(
            iocProvider: IocProvider,
            workflow: Workflow<TWorkflowModel>,
            stepData: StepData,
        ): TypeActivator {
            return BasicTypeActivator(
                BasicFunctionResolver(
                    PriorityParameterResolver(
                        listOf(
                            FixedValueParameterResolver(
                                ValueMatcher.canBeAssigned(),
                                workflow.model
                            ),
                            PriorityParameterResolver(stepData.data.map {
                                FixedValueParameterResolver(
                                    ValueMatcher.hasName<Any?>(it.key)
                                        .and(ValueMatcher.canBeAssigned()),
                                    it.value
                                )
                            }),
                            IocParameterResolver(iocProvider),
                            ImportedDataResolver { _, annotation ->
                                createRecursiveTypeActivator(
                                    iocProvider,
                                    workflow,
                                    stepData,
                                    annotation,
                                )
                            }
                        )
                    )
                )
            )
        }
        
        fun <TWorkflowModel> createRecursiveTypeActivator(
            iocProvider: IocProvider,
            workflow: Workflow<TWorkflowModel>,
            stepData: StepData,
            annotation: Import
        ): TypeActivator {
            val relevantData: Map<String, Any?> = when(annotation.prefix) {
                "" -> stepData.data
                else -> stepData.data
                    .filter { it.key.startsWith(annotation.prefix) }
                    .mapKeys { it.key.removePrefix(annotation.prefix) }
            }
            return BasicTypeActivator(
                BasicFunctionResolver(
                    PriorityParameterResolver(
                        listOf(
                            FixedValueParameterResolver(
                                ValueMatcher.canBeAssigned(),
                                workflow.model
                            ),
                            PriorityParameterResolver(relevantData.map {
                                FixedValueParameterResolver(
                                    ValueMatcher.hasName<Any?>(it.key)
                                        .and(ValueMatcher.canBeAssigned()),
                                    it.value
                                )
                            }),
                            IocParameterResolver(iocProvider),
                        )
                    )
                )
            )
        }
    }
}