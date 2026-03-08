package de.lise.fluxflow.springboot.rest

import de.lise.fluxflow.api.workflow.WorkflowNotFoundException
import de.lise.fluxflow.springboot.rest.workflow.step.StepNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController

@ControllerAdvice(annotations = [RestController::class])
class CommonExceptionControllerAdvice {
    @ExceptionHandler(WorkflowNotFoundException::class)
    fun handleWorkflowNotFound(
        e: WorkflowNotFoundException
    ): ResponseEntity<FluxFlowExceptionDto> {
        return ResponseEntity.status(
            HttpStatus.NOT_FOUND
        ).body(
            FluxFlowExceptionDto.WorkflowNotFound(e)
        )
    }

    @ExceptionHandler(StepNotFoundException::class)
    fun handleStepNotFound(
        e: StepNotFoundException
    ): ResponseEntity<FluxFlowExceptionDto> {
        return ResponseEntity.status(
            HttpStatus.NOT_FOUND
        ).body(
            FluxFlowExceptionDto.StepNotFound(e)
        )
    }
}