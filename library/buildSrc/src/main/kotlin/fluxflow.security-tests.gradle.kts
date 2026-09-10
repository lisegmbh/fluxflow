import de.lise.fluxflow.gradle.SecurityTestRequirements
import de.lise.fluxflow.gradle.MandatorySecurityTest
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions

plugins {
    java
}

val testSources = extensions.getByType<SourceSetContainer>().named("test")
val securityTestPatterns = listOf(
    "de/lise/fluxflow/mongo/security/baseline/**",
    "de/lise/fluxflow/mongo/security/production/**",
    "de/lise/fluxflow/mongo/security/prototype/**",
)
val securityRequirements = extensions.create<SecurityTestRequirements>("securityTests")
securityRequirements.requiredClasses.convention(emptyList())

// Test actions do not run for NO-SOURCE, so this prerequisite validates the
// compiled baseline before Gradle can skip the Test task.
val verifySecurityTestClasses = tasks.register("verifySecurityTestClasses") {
    dependsOn(tasks.named("testClasses"))
    doLast {
        val securityTask = tasks.named<Test>("securityTest").get()
        val platformOptions = securityTask.options as? JUnitPlatformOptions
        if (securityTask.filter.includePatterns.isNotEmpty() || securityTask.filter.excludePatterns.isNotEmpty() ||
            platformOptions?.includeTags?.isNotEmpty() == true || platformOptions?.excludeTags?.isNotEmpty() == true
        ) {
            throw GradleException("Security baseline does not allow test filters; run the regular test task for diagnostics.")
        }
        val classes = securityTask.candidateClassFiles
        if (classes.isEmpty) {
            throw GradleException("Security baseline classes are missing.")
        }
        val missingClasses = securityRequirements.requiredClasses.get().filter { className ->
            classes.none { it.invariantSeparatorsPath.endsWith("/${className.replace('.', '/')}.class") }
        }
        if (missingClasses.isNotEmpty()) {
            throw GradleException("Required security baseline classes were not selected: ${missingClasses.joinToString()}.")
        }
    }
}

val securityTest = tasks.register<MandatorySecurityTest>("securityTest") {
    description = "Runs the mandatory security baseline tests."
    group = "verification"
    dependsOn(verifySecurityTestClasses)
    testClassesDirs = testSources.get().output.classesDirs
    classpath = testSources.get().runtimeClasspath
    include(securityTestPatterns)
    useJUnitPlatform()
    // Container availability is external state and is not represented by Gradle inputs.
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
    doFirst {
        if (!reports.junitXml.required.get()) {
            throw GradleException("Security baseline XML report is required.")
        }
    }
    doLast {
        val reportsPresent = reports.junitXml.outputLocation.get().asFile
            .listFiles { file -> file.name.startsWith("TEST-") && file.extension == "xml" }
        if (reportsPresent.isNullOrEmpty()) {
            throw GradleException("Security baseline XML report is required.")
        }
    }
    addTestListener(object : TestListener {
        private val executedClasses = mutableSetOf<String>()

        override fun beforeSuite(suite: TestDescriptor) = Unit
        override fun beforeTest(test: TestDescriptor) = Unit
        override fun afterTest(test: TestDescriptor, result: TestResult) {
            test.className?.let { executedClasses.add(it) }
        }

        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null && result.skippedTestCount > 0) {
                throw GradleException("Security baseline must execute without skipped tests.")
            }
            if (suite.parent == null) {
                if (result.testCount == 0L) {
                    throw GradleException("Security baseline must execute at least one test.")
                }
                if (result.failedTestCount > 0) {
                    throw GradleException("Security baseline must execute without failed tests.")
                }
                val missingClasses = securityRequirements.requiredClasses.get() - executedClasses
                if (missingClasses.isNotEmpty()) {
                    throw GradleException("Required security baseline classes did not execute: ${missingClasses.joinToString()}.")
                }
            }
        }
    })
}

tasks.named("check") {
    dependsOn(securityTest)
}
