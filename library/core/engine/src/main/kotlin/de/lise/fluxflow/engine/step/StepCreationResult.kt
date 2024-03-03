package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.automation.Automation

data class StepCreationResult(
    val step: Step,
    val onCreatedAutomations: Collection<Automation>,
)