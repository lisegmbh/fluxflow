package de.lise.fluxflow.stereotyped.step.automation

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.automation.Automation
import de.lise.fluxflow.api.step.automation.AutomationDefinition

class ReflectedAutomationDefinition<T>(
    private val instance: T,
    private val methodCaller: AutomationFunctionCaller<T>
) : AutomationDefinition {
    override fun createAutomation(step: Step): Automation {
        return ReflectedAutomation(
            step,
            this,
            instance,
            methodCaller
        )
    }
}