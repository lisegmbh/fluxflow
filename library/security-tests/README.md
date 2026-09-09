# Security baseline tests

This source directory is shared by the Spring Boot 3 and Spring Boot 4 Mongo test
modules. It is not a published module. PR01 added the reproducible characterization
harness. PR03 converts the step and job scenarios into permanent rejection regressions;
the Mongo model scenario remains a characterization until its dedicated fix.

## Run the mandatory baseline

From `library`, with JDK 17 and a working Docker-compatible container runtime:

```powershell
.\gradlew.bat :springboot:springboot-mongo:securityTest :springboot4:springboot-mongo:securityTest
```

Both module `check` tasks also run `securityTest`. The gate requires the complete
baseline suite, rejects skips and missing test/report output, and executes again
even when source files have not changed. A container startup failure is a failure,
not a reason to disable tests. No production Mongo URI or data is used.

The existing conditional integration tests are not the security gate. Their event
propagation probes do not establish that untrusted types cannot be instantiated.

## Contract and evidence

| Plan ID | Boundary | Current expectation |
|---|---|---|
| R01 | Persisted step kind through real step activation | Reject an unregistered kind before class initialization/construction |
| R02 | Persisted job kind through real job activation | Reject an unregistered kind before class initialization/construction |
| R03 | Raw Mongo change to `model._class`, then `WorkflowPersistence.find` | Characterize materialization of the harmless model |
| R04 | Raw Mongo change only to root `_class`, then the same read | Observe the actual root-type behavior independently of R03 |
| R05 | Fixture in a fresh class loader | Distinguish class initialization from constructor invocation |
| R06 | Gradle task boundary | Missing, filtered, skipped or failing required tests fail the gate |

Marker fixtures record local test events only. A fresh loader is needed to repeat
static initialization: resetting a counter cannot reset a JVM class initializer.
Each Mongo scenario starts from a valid model and derives the collection name
from the real Spring Data mapping. Tampering uses the raw driver; the observation
uses the real Fluxflow persistence/activation boundary.

R01 and R02 always require rejection and no marker event. For the separate R03
before-fix safety demonstration, use `-PsecurityExpectRejection=true`. That mode must
still fail while Mongo model resolution is vulnerable. It is not the normal CI mode
and must never be described as a successful security verification. The dedicated
Mongo fix replaces that remaining characterization with a mandatory rejection
regression; fixture positive controls remain.

XML reports are written to each Mongo module's
`build/test-results/securityTest/` directory. Record the Git revision, commands,
resolved runtime dependencies, test counts, failures and skips with the review.
The complete report set must contain the required suites in both compatibility
lines. Green R01/R02 reports prove that activation rejects the report witnesses;
R03 remains open until its own rejection regression is green.

## Build gate tests

```powershell
.\gradlew.bat -p buildSrc test
```

These TestKit tests execute small Gradle builds. Deliberate assertion and startup
failures are expected by their parent tests, so the parent suite remains green.
Do not weaken the gate or skip container tests to accommodate an unavailable CI
runtime. The Jenkins agent needs a working runtime before the mandatory check can
succeed.

## Resolved dependency baseline

The PR01 verification resolved the following `testRuntimeClasspath` versions.
These versions document the framework boundary exercised by the shared tests:

| Dependency | Spring Boot 3 module | Spring Boot 4 module |
|---|---:|---:|
| Spring Data MongoDB | 4.5.5 | 5.0.5 |
| MongoDB synchronous driver | 5.5.2 | 5.6.5 |
| JUnit Jupiter engine | 5.12.2 | 6.0.3 |
| Testcontainers | 1.21.4 | 2.0.5 |

Both lines run against the pinned `mongo:8.0.12` image. Testcontainers 1.21.4 is
required on the Boot 3 line for compatibility with current Docker Engine API
versions; the previously resolved 1.21.3 client failed before container startup.
