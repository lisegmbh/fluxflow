package de.lise.fluxflow.api.proxy.step

import de.lise.fluxflow.api.step.Step

interface StepAccessor {
    @Suppress("PropertyName") // This intentionally uses an uncommon property name, in order to avoid conflicts
    val _proxyStep: Step
}