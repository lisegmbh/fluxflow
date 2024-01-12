package de.lise.fluxflow.api.proxy.step.action

import de.lise.fluxflow.api.proxy.step.StepAccessor
import de.lise.fluxflow.api.step.stateful.action.ActionKind
import de.lise.fluxflow.api.step.stateful.action.ActionService
import net.bytebuddy.implementation.bind.annotation.RuntimeType
import net.bytebuddy.implementation.bind.annotation.This

class ActionInvocationProxy(
    private val actionService: ActionService,
    private val actionKind: ActionKind
) {
    @Suppress("unused") // Used by ByteBuddy
    @RuntimeType
    fun intercept(@This instance: StepAccessor): Any? {
        val action = actionService.getAction(
            instance._proxyStep,
            actionKind
        )!!
        actionService.invokeAction(action)
        return null // TODO: Return something
    }
}