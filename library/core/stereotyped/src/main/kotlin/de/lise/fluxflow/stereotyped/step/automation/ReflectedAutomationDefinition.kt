package de.lise.fluxflow.stereotyped.step.automation

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.automation.Automation
import de.lise.fluxflow.api.step.automation.AutomationDefinition
import de.lise.fluxflow.stereotyped.step.bind

class ReflectedAutomationDefinition<T>(
    private val methodCaller: AutomationFunctionCaller<T>
) : AutomationDefinition {
    override fun createAutomation(step: Step): Automation {
        val instance = step.bind<T>()!!
        return ReflectedAutomation(
            step,
            this,
            instance,
            methodCaller
        )
    }
}