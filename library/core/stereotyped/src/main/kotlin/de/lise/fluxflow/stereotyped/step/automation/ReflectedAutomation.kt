package de.lise.fluxflow.stereotyped.step.automation

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.automation.Automation
import de.lise.fluxflow.api.step.automation.AutomationDefinition

class ReflectedAutomation<T>(
    override val step: Step,
    override val definition: AutomationDefinition,
    private val instance: T,
    private val methodCaller: AutomationFunctionCaller<T>
) : Automation {
    override fun execute(): Continuation<*> {
        return methodCaller.call(step, instance)
    }
}