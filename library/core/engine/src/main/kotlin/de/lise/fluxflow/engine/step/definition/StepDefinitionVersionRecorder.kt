package de.lise.fluxflow.engine.step.definition

import de.lise.fluxflow.api.step.StepDefinition
import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.versioning.Version
import de.lise.fluxflow.api.versioning.VersionRecorder
import org.slf4j.LoggerFactory

class StepDefinitionVersionRecorder(
    private val stepDefinitionService: StepDefinitionService,
    private val alreadyRecordedVersions: MutableSet<Pair<StepKind, Version>> = mutableSetOf()
) : VersionRecorder<StepDefinition> {
    override fun record(element: StepDefinition) {
        val cachingKey = Pair(element.kind, element.version)
        if(alreadyRecordedVersions.contains(cachingKey)) {
            Logger.debug("Skip persisting/updating step definition for {}, as it has been process previously.", cachingKey)
            return
        }
        stepDefinitionService.save(element)
        alreadyRecordedVersions.add(cachingKey)
        
    }

    private companion object {
        val Logger = LoggerFactory.getLogger(StepDefinitionVersionRecorder::class.java)!!
    }
}