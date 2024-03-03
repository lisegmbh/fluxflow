package de.lise.fluxflow.examples.ideas.workflow

import de.lise.fluxflow.stereotyped.step.Step
import de.lise.fluxflow.stereotyped.step.action.Action
import de.lise.fluxflow.stereotyped.step.data.Data

@Step
class SubmitIdeaStep {
    @Data
    var summary: String = ""
    @Data
    var author: String = ""
    @Data
    var description: String = ""
    
    @Action
    fun submit(): IdeaReviewStep {
        return IdeaReviewStep(
            summary,
            author,
            description
        )
    }
}