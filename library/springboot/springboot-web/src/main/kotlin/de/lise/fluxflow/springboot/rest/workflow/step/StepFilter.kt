package de.lise.fluxflow.springboot.rest.workflow.step

import de.lise.fluxflow.api.step.Status
import org.springframework.util.MultiValueMap

data class StepFilter(
    val kind: Set<String>?,
    val status: Set<Status>?,
    val data: MultiValueMap<String, Any?>
)