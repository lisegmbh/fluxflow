package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.step.stateful.data.ModificationPolicy

/**
 * Step properties can be annotated with [Data] to indicate,
 * that they can be used to set or get step data. 
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Data(
    /**
     * By default, the properties' name is used as an identifier.
     * This attribute can be used to overwrite the default name.
     */
    val identifier: String = "",
    val persistenceType: PersistenceType = PersistenceType.Auto,
    val modificationPolicy: ModificationPolicy = ModificationPolicy.InheritSetting,
)