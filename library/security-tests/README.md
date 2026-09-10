# Security tests

This source directory is shared by the Spring Boot 3 and Spring Boot 4 Mongo test
modules. It is not a published module. It contains the reproducible activation baseline
and permanent production regression tests for trusted Mongo type materialization.

## Run the mandatory security suite

From `library`, with JDK 17 and a working Docker-compatible container runtime:

```powershell
.\gradlew.bat :springboot:springboot-mongo:securityTest :springboot4:springboot-mongo:securityTest
```

Both module `check` tasks also run `securityTest`. The gate requires the complete
security suite, rejects skips and missing test/report output, and executes again
even when source files have not changed. A container startup failure is a failure,
not a reason to disable tests. No production Mongo URI or data is used.

The existing conditional integration tests are not the security gate. Their event
propagation probes do not establish that untrusted types cannot be instantiated.

## Contract and evidence

| Plan ID | Boundary | Current expectation |
|---|---|---|
| R01 | Persisted step kind through real step activation | Reject an unregistered kind before class initialization/construction |
| R02 | Persisted job kind through real job activation | Reject an unregistered kind before class initialization/construction |
| R03 | Raw Mongo change to `model._class`, then `WorkflowPersistence.find` | Reject the model before class initialization or construction |
| R04 | Raw Mongo change only to the root type key, then the same read | Reject an incompatible root before materialization |
| R05 | Fixture in a fresh class loader | Distinguish class initialization from constructor invocation |
| R06 | Gradle task boundary | Missing, filtered, skipped or failing required tests fail the gate |
| V01 | Legacy step/job value type maps and migration | Reject an unregistered enum name before initialization and before migration writes |
| V02 | Current typed records in steps, jobs and definitions | Reject an unregistered JVM type reference before enum initialization |
| V03 | Registered values and fixed built-ins | Preserve enums, aliases, collections, dates, instants and nulls without TCCL lookup |
| V04 | Malformed value metadata and writes | Fail with defined errors and reject unregistered values before create/save writes |
| V05 | Multiple application contexts | Keep immutable value registries isolated under parallel conversion |

Marker fixtures record local test events only. A fresh loader is needed to repeat
static initialization: resetting a counter cannot reset a JVM class initializer.
Each Mongo scenario starts from a valid model and derives the collection name
from the real Spring Data mapping. Tampering uses the raw driver; the observation
uses the real Fluxflow persistence/activation boundary.

R01, R02 and R03 require rejection and no marker event. The security task has no
compatibility mode that accepts witness materialization.

XML reports are written to each Mongo module's
`build/test-results/securityTest/` directory. Record the Git revision, commands,
resolved runtime dependencies, test counts, failures and skips with the review.
The complete report set must contain the required suites in both compatibility
lines. Green R01/R02 reports prove that activation rejects the report witnesses. Green
R03 reports prove the same at the production Mongo conversion boundary.

V01 and V02 use a separate enum witness because enum constant access initializes its class even
when Mongo stores the value itself as a string. The production contract mutates only the legacy
`typeName` or the matching typed-record JVM entry, then reads through the real persistence bean.
The companion converter contract covers structural corruption and bounded graph traversal without
requiring Mongo. Both contracts are mandatory in each Spring compatibility line.

## Production Mongo conversion boundary

FluxFlow uses an internal Mongo access object with its own `MappingMongoConverter` and
`MongoTemplate`. It clones the application's configured
mapping converter through Spring Data's public `with(MongoDatabaseFactory)` API, replaces
only the type mapper, and keeps the same `MongoDatabaseFactory`. FluxFlow constructs its
workflow repository and query fragment from this internal template. The template is held
inside the access object instead of being exposed as an application `MongoTemplate` bean,
so application repositories and Boot's conditional bean graph remain unchanged. The
access object is created during the real Boot AutoConfiguration context;
an application-style repository remains bound to the host converter while the internal
FluxFlow repository uses the restricted converter.

This boundary was selected over lifecycle listeners because converter `read` and
`project` are synchronous materialization points. Listeners can be disabled, can run
asynchronously, and do not protect direct converter calls. The production contract proves
rejection through the isolated converter and its version adapter with lifecycle events disabled,
with an asynchronous event multicaster, and when a listener swallows its own rejection.

The Mongo type mapper is built only from `TypeRegistry` entries with role `MODEL` or
`VALUE`, plus the trusted `WorkflowDocument` root. The guard assigns the top-level
`model.<type-key>` discriminator to `MODEL` and discriminators recursively nested below
the model to `VALUE`. Unknown, empty, non-string, wrong-role, or conflicting aliases fail
closed before Spring Data can resolve a class. The persisted `modelType` field is not a
trust anchor. Existing FQCN registrations and logical aliases are both accepted, and no
BSON rewriting is used.

The shared contract runs against Spring Data MongoDB 4.x and 5.x through two small
`TypeInformationMapper` adapters. It covers direct converter reads, repository and aggregation
reads, nested lists/maps/nulls, scalar and
container models, custom type keys, custom conversions, alias conflicts, unchanged host
beans, shared transaction rollback, context isolation and traversal limits. The mandatory
`securityTest` task requires the production suites and fails if a suite is missing,
skipped, or failing.

The internal template preserves the host mapping context, custom conversions, database
factory, configured type metadata key and read preference. It stays outside the host bean
graph. Applications can provide ordered `FluxFlowMongoTemplateCustomizer` beans for
additional internal-template settings that Spring Data does not expose for safe copying.

Value reconstruction runs after the guarded Mongo conversion boundary and uses a
`ValueTypeConverter` built from the same context-local `TypeRegistry`. Its fixed built-in table and
exact `VALUE` registrations replace the former context-class-loader lookup in `SimpleType`.
Step, job, step-definition, and type-record migration paths share this converter. Invalid record
references, key sets, collection metadata, cycles, and excessive graphs fail before raw values can
escape or persistence writes can occur. A registration authorizes only its exact persisted key.
Because map records do not describe nested value types, enum values inside maps are rejected before
write instead of being silently restored as strings. Container normalization is limited to fixed
built-in aliases; an application container registration requires an actual instance of that type.
The default writer stores a custom value's canonical JVM name, so that exact key must be registered
in addition to any logical aliases used by historical records.

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
