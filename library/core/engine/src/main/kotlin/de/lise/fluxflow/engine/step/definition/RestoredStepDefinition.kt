package de.lise.fluxflow.engine.step.definition

import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.step.automation.AutomationDefinition
import de.lise.fluxflow.api.step.stateful.StatefulStepDefinition
import de.lise.fluxflow.api.step.stateful.action.ActionDefinition
import de.lise.fluxflow.api.versioning.Version
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.engine.step.data.RestoredData
import de.lise.fluxflow.engine.step.data.RestoredDataDefinition
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.definition.StepDefinitionData

class RestoredStepDefinition(
    override val kind: StepKind,
    override val version: Version,
    override val metadata: Map<String, Any>,
    override val data: List<RestoredDataDefinition>,
) : StatefulStepDefinition {
    override val actions: List<ActionDefinition>
        get() = emptyList()
    override val onCreatedAutomations: Set<AutomationDefinition>
        get() = emptySet()
    override val onCompletedAutomations: Set<AutomationDefinition>
        get() = emptySet()

    constructor(
        definitionData: StepDefinitionData,
        stepData: StepData?
    ) : this(
        StepKind(definitionData.kind),
        Version.parse(definitionData.version),
        definitionData.metadata,
        definitionData.data.map {
            RestoredDataDefinition(
                it,
                stepData?.data?.get(it.kind)
            )
        },
    )

    override fun createStep(
        workflow: Workflow<*>,
        stepIdentifier: StepIdentifier,
        status: Status,
        metadata: Map<String, Any>?
    ): RestoredStepImpl {
        val dataList = mutableListOf<RestoredData>()
        val step = RestoredStepImpl(
            stepIdentifier,
            this,
            workflow,
            status,
            metadata ?: this.metadata,
            dataList
        )
        dataList.addAll(
            data.map { it.createData(step) }
        )

        return step
    }
}