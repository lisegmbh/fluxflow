package de.lise.fluxflow.springboot.rest.workflow.step.data

import de.lise.fluxflow.rest.workflow.step.data.StepDataSpecDto

data class DefaultStepDataSpecDto(
    override val value: Any?
) : StepDataSpecDto