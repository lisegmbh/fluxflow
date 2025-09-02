package de.lise.fluxflow.persistence.step.definition

interface StepDefinitionPersistence {
    /**
     * Saves the given [stepDefinition].
     * A pre-existing step definition with the same [StepDefinitionData.kind] and [StepDefinitionData.version],
     * is going to be replaced/updated.
     */
    fun save(stepDefinition: StepDefinitionData): StepDefinitionData

    /**
     * Searches for a step definitions using its kind and version.
     * @return The found step definition or `null`, if there is no step definition for the provided [stepKind] and [version].
     */
    fun findForKindAndVersion(
        stepKind: String,
        version: String
    ): StepDefinitionData?
}