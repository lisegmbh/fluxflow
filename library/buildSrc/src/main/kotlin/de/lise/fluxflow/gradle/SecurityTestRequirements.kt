package de.lise.fluxflow.gradle

import org.gradle.api.provider.ListProperty

/** Declares the baseline suites that must participate in every security gate run. */
abstract class SecurityTestRequirements {
    abstract val requiredClasses: ListProperty<String>
}
