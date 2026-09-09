package de.lise.fluxflow.gradle

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SecurityTestsTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `check should run security tests and produce a report`() {
        fixture()
        baseline("@Test void baseline() {}")

        val result = runner("check").build()

        assertThat(result.task(":securityTest")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(File(projectDir, "build/test-results/securityTest/TEST-de.lise.fluxflow.mongo.security.baseline.BaselineTest.xml"))
            .exists()
    }

    @Test
    fun `security gate should reject skipped tests`() {
        fixture()
        baseline("@Test @Disabled void skipped() {} @Test void passing() {}")

        val result = runner("securityTest").buildAndFail()

        assertThat(result.output).contains("Security baseline must execute without skipped tests")
    }

    @Test
    fun `security gate should reject missing sources instead of reporting NO-SOURCE`() {
        fixture()

        val result = runner("securityTest").buildAndFail()

        assertThat(result.output).contains("Security baseline classes are missing")
    }

    @Test
    fun `security gate should reject an empty test selection`() {
        fixture()
        baseline("@Test void baseline() {}")

        val result = runner("securityTest", "--tests", "MissingTest").buildAndFail()

        assertThat(result.output).contains("No tests found for given includes")
    }

    @Test
    fun `security gate should reject exclusions that remove every test class`() {
        fixture("securityTest { exclude '**/*' }")
        baseline("@Test void baseline() {}")

        val result = runner("securityTest").buildAndFail()

        assertThat(result.output).contains("Security baseline classes are missing")
    }

    @Test
    fun `security gate should fail an assertion`() {
        fixture()
        baseline("@Test void baseline() { Assertions.fail(\"regression detected\"); }")

        val result = runner("securityTest").buildAndFail()

        assertThat(result.task(":securityTest")?.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("There were failing tests")
    }

    @Test
    fun `security gate should fail when container initialization fails`() {
        fixture()
        baseline("""
            @BeforeAll static void startContainer() { throw new IllegalStateException("container startup failed"); }
            @Test void baseline() {}
        """.trimIndent())

        val result = runner("securityTest").buildAndFail()

        assertThat(result.task(":securityTest")?.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("There were failing tests")
    }

    @Test
    fun `security gate should require XML evidence`() {
        fixture("securityTest { reports.junitXml.required = false }")
        baseline("@Test void baseline() {}")

        val result = runner("securityTest").buildAndFail()

        assertThat(result.output).contains("Security baseline XML report is required")
    }

    @Test
    fun `security gate should execute again even when inputs are unchanged`() {
        fixture()
        baseline("@Test void baseline() {}")
        runner("securityTest").build()

        val result = runner("securityTest", "--build-cache").build()

        assertThat(result.task(":securityTest")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `security gate should reject one missing required class`() {
        fixture("""
            securityTests.requiredClasses = [
                'de.lise.fluxflow.mongo.security.baseline.BaselineTest',
                'de.lise.fluxflow.mongo.security.baseline.MongoBaselineTest'
            ]
        """.trimIndent())
        baseline("@Test void baseline() {}")

        val result = runner("securityTest").buildAndFail()

        assertThat(result.output).contains("Required security baseline classes were not selected", "MongoBaselineTest")
    }

    private fun fixture(extra: String = "") {
        File(projectDir, "settings.gradle").writeText("rootProject.name = 'security-fixture'")
        File(projectDir, "build.gradle").writeText(
            """
            plugins { id 'java'; id 'fluxflow.security-tests' }
            repositories { mavenCentral() }
            dependencies {
                testImplementation platform('org.junit:junit-bom:6.1.3')
                testImplementation 'org.junit.jupiter:junit-jupiter'
                testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
            }
            tasks.withType(Test).configureEach { useJUnitPlatform() }
            $extra
            """.trimIndent()
        )
    }

    private fun baseline(body: String) {
        val source = File(projectDir, "src/test/java/de/lise/fluxflow/mongo/security/baseline/BaselineTest.java")
        source.parentFile.mkdirs()
        source.writeText(
            """
            package de.lise.fluxflow.mongo.security.baseline;
            import org.junit.jupiter.api.*;
            public class BaselineTest { $body }
            """.trimIndent()
        )
    }

    private fun runner(vararg arguments: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*arguments, "--stacktrace")
}
