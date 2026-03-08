package de.lise.fluxflow.rest.workflow.step

import de.lise.fluxflow.rest.StatefulResourceDto

interface StepDto: StatefulResourceDto<StepMetadataDto, StepSpecDto, StepStatusDto> {
    override val kind: String
        get() = "step"
}