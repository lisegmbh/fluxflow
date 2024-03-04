package de.lise.fluxflow.examples.ideas.workflow

import de.lise.fluxflow.stereotyped.step.Step
import de.lise.fluxflow.stereotyped.step.data.Data

@Step
class IdeaReviewStep(
    val summary: String,
    val author: String,
    val description: String
) {
    @Data
    var feedback: String = ""
}