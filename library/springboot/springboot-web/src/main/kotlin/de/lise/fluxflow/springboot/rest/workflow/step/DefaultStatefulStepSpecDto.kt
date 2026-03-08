package de.lise.fluxflow.springboot.rest.workflow.step

import de.lise.fluxflow.rest.workflow.step.StatefulStepSpecDto

data class DefaultStatefulStepSpecDto(
    override val data: Map<String, Any?>
) : StatefulStepSpecDto