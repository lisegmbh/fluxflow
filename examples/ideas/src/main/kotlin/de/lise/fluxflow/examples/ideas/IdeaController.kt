package de.lise.fluxflow.examples.ideas

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.examples.ideas.workflow.SubmitIdeaStep
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/idea")
class IdeaController(
    private val workflowService: WorkflowService
) {
    @PostMapping
    fun start(): Workflow<Any> {
        return workflowService.start(
            IdeaWorkflowData(
                Instant.now()
            ),
            Continuation.step(SubmitIdeaStep())
        )
    }
}