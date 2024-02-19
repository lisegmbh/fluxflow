package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.stereotyped.workflow.ModelListener

data class WorkflowModelWithListener(
    var someProp: String = "",
    var anotherProp: Boolean = false,
    var aThirdProp: Int = 42,
    var invokeOnSomeOrAnotherPropChangeRecord: InvocationRecord? = null,
    var invokeOnStringLengthChangeRecord: InvocationRecord? = null,
    var invokeOnAnyChangeRecord: InvocationRecord? = null,
    var modifyingListenerRecord: InvocationRecord? = null,
    var modifiedSomePropRecord: InvocationRecord? = null
) {

    @ModelListener
    fun invokeOnAnyChange(
        stepService: StepService?,
        workflow: Workflow<*>,
        oldModel: WorkflowModelWithListener,
        newModel: WorkflowModelWithListener
    ) {
        invokeOnAnyChangeRecord = InvocationRecord(
            oldModel.copy(),
            newModel.copy()
        ).apply {
            hadStepService = stepService != null
            workflowIdentifier = workflow.identifier
        }
    }

    @ModelListener(
        "#old.someProp.length != #new.someProp.length",
        selectorReturnsDecision = true
    )
    fun invokeWhenStringLengthChanges() {
        invokeOnStringLengthChangeRecord = InvocationRecord()
    }

    @ModelListener("#current.someProp")
    @ModelListener("#current.anotherProp")
    fun invokeOnSomeProps(
        stepService: StepService,
        oldModel: WorkflowModelWithListener,
        newModel: WorkflowModelWithListener
    ) {
        invokeOnSomeOrAnotherPropChangeRecord = InvocationRecord(
            oldModel.copy(),
            newModel.copy()
        ).apply { hadStepService = true }
    }

    @ModelListener("#current.someProp")
    fun onSomeProp() {
        modifiedSomePropRecord = InvocationRecord()
    }

    @ModelListener(
        "#new.aThirdProp == -42",
        selectorReturnsDecision = true
    )
    fun changePropOnInvocation() {
        modifyingListenerRecord = InvocationRecord()
        someProp = "$someProp and $someProp"
    }

    data class InvocationRecord(
        var oldModel: WorkflowModelWithListener? = null,
        var newModel: WorkflowModelWithListener? = null,
        var hadStepService: Boolean? = null,
        var workflowIdentifier: WorkflowIdentifier? = null
    )
}