package de.lise.fluxflow.stereotyped.step.action

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.action.Action

class ReflectedAction<TInstance>(
    override val step: Step,
    override val definition: ReflectedActionDefinition<TInstance>,
    private val instance: TInstance,
    private val actionCaller: ActionCaller<TInstance>
) : Action {
    override fun execute(): Continuation<*> {
        return actionCaller.call(step, instance)
    }

}
