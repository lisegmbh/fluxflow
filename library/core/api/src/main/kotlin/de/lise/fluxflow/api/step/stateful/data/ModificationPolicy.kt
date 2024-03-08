package de.lise.fluxflow.api.step.stateful.data

enum class ModificationPolicy {
    /**
     * Allows inactive step data modification.
     */
    AllowInactiveModification,

    /**
     * Prevents inactive step data modification.
     */
    PreventInactiveModification,

    /**
     * Inherits global setting `fluxflow.data.allow-inactive-modification`.
     */
    InheritSetting,
}
