# Security baseline tests

This source directory is shared by the Spring Boot 3 and Spring Boot 4 Mongo test
modules. It is not a published module. PR01 adds reproducible characterization
tests, not a security fix. No production resolver or converter is changed.

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

| Plan ID | Boundary | PR01 expectation |
|---|---|---|
| R01 | Persisted step kind through real step activation | Characterize initialization/construction of the harmless marker |
| R02 | Persisted job kind through real job activation | Characterize initialization/construction of the harmless marker |
| R03 | Raw Mongo change to `model._class`, then `WorkflowPersistence.find` | Characterize materialization of the harmless model |
| R04 | Raw Mongo change only to root `_class`, then the same read | Observe the actual root-type behavior independently of R03 |
| R05 | Fixture in a fresh class loader | Distinguish class initialization from constructor invocation |
| R06 | Gradle task boundary | Missing, filtered, skipped or failing required tests fail the gate |

Marker fixtures record local test events only. A fresh loader is needed to repeat
static initialization: resetting a counter cannot reset a JVM class initializer.
Each Mongo scenario starts from a valid model and derives the collection name
from the real Spring Data mapping. Tampering uses the raw driver; the observation
uses the real Fluxflow persistence/activation boundary.

For the separate before-fix safety demonstration, use the same scenarios with
`-PsecurityExpectRejection=true`. That mode expects rejection and no marker event;
it must fail on the vulnerable baseline for R01–R03. It is not the normal CI mode
and must never be described as a successful security verification. The relevant
fix PR replaces each temporary production characterization with a mandatory
rejection regression; fixture positive controls remain.

XML reports are written to each Mongo module's
`build/test-results/securityTest/` directory. Record the Git revision, commands,
resolved runtime dependencies, test counts, failures and skips with the review.
The complete report set must contain the required suites in both compatibility
lines. A green PR01 report means the reproduction harness works; the findings
remain open.

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
