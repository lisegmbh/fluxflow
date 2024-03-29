package de.lise.fluxflow.stereotyped.step.action

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.action.Action
import de.lise.fluxflow.api.step.stateful.action.ActionDefinition
import de.lise.fluxflow.api.step.stateful.action.ActionKind
import de.lise.fluxflow.api.validation.ValidationBehavior
import kotlin.reflect.KClass

class ReflectedActionDefinition<TInstance>(
    override val kind: ActionKind,
    override val metadata: Map<String, Any>,
    override val beforeExecutionValidation: ValidationBehavior,
    override val validationGroups: Set<KClass<*>>,
    private val instance: TInstance,
    private val actionCaller: ActionCaller<TInstance>
) : ActionDefinition {
    override fun createAction(step: Step): Action {
        return ReflectedAction(
            step,
            this,
            instance,
            actionCaller
        )
    }
}