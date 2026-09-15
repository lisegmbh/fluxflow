package de.lise.fluxflow.gradle

import org.gradle.api.GradleException
import org.gradle.api.tasks.testing.Test

/** Keeps Gradle's command-line test selection from weakening the mandatory gate. */
abstract class MandatorySecurityTest : Test() {
    override fun setTestNameIncludePatterns(testNamePattern: List<String>): Test {
        if (testNamePattern.isNotEmpty()) {
            throw GradleException("Security baseline does not allow test filters; run the regular test task for diagnostics.")
        }
        return super.setTestNameIncludePatterns(testNamePattern)
    }
}
