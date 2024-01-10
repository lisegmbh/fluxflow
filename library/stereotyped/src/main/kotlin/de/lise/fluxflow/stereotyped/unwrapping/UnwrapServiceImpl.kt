package de.lise.fluxflow.stereotyped.unwrapping

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.stereotyped.step.ReflectedStatefulStepDefinition

class UnwrapServiceImpl : UnwrapService {
    override fun <T : Any> unwrap(step: Step): T {
        return when(val definition = step.definition) {
            is ReflectedStatefulStepDefinition -> definition.instance as T
            else -> throw IllegalUnwrapException(
                "Could not unwrap a step that isn't based on a JVM object. Instead the definition's type is: ${definition::class.simpleName}",
                step
            )
        }
    }
}