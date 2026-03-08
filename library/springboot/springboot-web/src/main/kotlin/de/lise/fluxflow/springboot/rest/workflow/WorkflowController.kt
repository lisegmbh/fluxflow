package de.lise.fluxflow.springboot.rest.workflow

import de.lise.fluxflow.api.ExperimentalApi
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.rest.mapping.Mapping
import de.lise.fluxflow.rest.mapping.Mapping.Companion.mapWith
import de.lise.fluxflow.rest.workflow.WorkflowDto
import org.springframework.web.bind.annotation.*

@ExperimentalApi
@RestController
@RequestMapping($$"${fluxflow.api.prefix:/api}/workflow")
class WorkflowController(
    private val workflowMapping: Mapping<Workflow<*>, WorkflowDto>,
    private val workflowService: WorkflowService,
) {
    @GetMapping
    fun getAll(): List<WorkflowDto> {
        return workflowService.getAll().mapWith(workflowMapping)
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: String
    ): WorkflowDto {
        return workflowService.get<Any?>(
            WorkflowIdentifier(
                id
            )
        ).mapWith(workflowMapping)
    }

    @DeleteMapping("/{id}")
    fun deleteById(
        @PathVariable id: String
    ) {
        workflowService.delete(
            WorkflowIdentifier(
                id
            )
        )
    }
}