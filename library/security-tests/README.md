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

The plan identifiers are mapped to executable tests below. Methods declared by an abstract Mongo
contract run through the corresponding `Boot3...IT` and `Boot4...IT` wrappers. Tests under
`springboot/src/testShared` likewise run in both Spring compatibility modules.

| ID | Boundary | Executable evidence |
|---|---|---|
| R01 | Persisted step kind | `ActivationBaselineTest.R01 persisted unregistered step kind is rejected before initialization` |
| R02 | Persisted job kind | `ActivationBaselineTest.R02 persisted unregistered job kind is rejected before initialization` |
| R03 | Workflow model discriminator | `AbstractProductionMongoSecurityContractIT.R03 D02 production workflow reads reject an unregistered model before materialization` |
| R04 | Root discriminator from the report scenario | `AbstractProductionMongoSecurityContractIT.R04 root discriminator tampering is rejected on production workflow read` |
| R05 | Initializer/constructor witness | `SecurityWitnessTest.R05 witness records initialization and construction independently` |
| R06 | Mandatory Gradle boundary | `SecurityTestsTest.security gate should reject skipped tests`, `security gate should reject missing sources instead of reporting NO-SOURCE`, `security gate should fail when container initialization fails`, and `security gate should require XML evidence` |
| M01 | Annotation scan roots | `FluxFlowTypeRegistryFactoryTest.M01 should discover annotated types from a default application package` and `M01 should normalize explicit and overlapping scan roots` |
| M02 | Deterministic merge and conflicts | `TypeRegistryTest.M02 should merge identical registrations independent of input order`, `M02 should reject conflicting registrations with stable diagnostics`, `TypeManifestLoaderTest.M02 should load and merge manifests from multiple artifacts deterministically`, and `FluxFlowTypeManifestPluginTest.M02 O06 should generate deterministic manifest bytes and include them in the jar` |
| M03 | Explicit and malformed declarations | `TypeManifestTest.M03 should reject malformed manifests with origin and line`, `TypeManifestTest.M03 should reject unsupported manifest versions`, `FluxFlowTypeRegistryFactoryTest.M03 should merge an explicit unannotated type registration`, `FluxFlowTypeManifestPluginTest.M03 O06 should fail generation when an explicit class is missing`, and `FluxFlowTypeManifestPluginTest.M03 O06 should reject an explicit type absent from the runtime classpath` |
| M04 | No initialization during discovery | `TypeRegistryTest.M04 should resolve manifest classes without initializing them`, `FluxFlowTypeRegistryFactoryTest.M04 should scan annotated types without initializing them`, and `FluxFlowTypeManifestPluginTest.M04 should inspect annotated classes without initialization` |
| M05 | Exact aliases, FQCNs, and nested names | `TypeRegistryTest.M05 should resolve aliases and legacy keys only in their declared roles`, `M05 should reject undeclared key variations without consulting the classloader`, `StepTypeResolverImplTest.M05 should resolve only registered step FQCNs and aliases`, and `JobActivationServiceTest.M05 M10 should activate registered job FQCNs and aliases` |
| M06 | Unknown and wrong-role activation | `StepTypeResolverImplTest.M06 should reject a step registered only for another role without initializing it`, `StepTypeResolverImplTest.M06 legacy constructor should reject an unregistered class without initializing it`, `JobActivationServiceTest.M06 should reject a job registered only for another role without initializing it`, and `JobActivationServiceTest.M06 legacy service constructor should reject an unregistered class without initializing it` |
| M07 | Immutable, context-local registry | `TypeRegistryTest.M07 should isolate immutable registries and concurrent lookups`, `FluxFlowTypeRegistryFactoryTest.M07 should keep explicit registrations local to their application context`, `FluxFlowTypeRegistryFactoryTest.M07 should scan with the application context class loader`, and `StepTypeResolverImplTest.M07 should isolate resolver mappings from later mutations` |
| M08 | Atomic load and activation errors | `TypeRegistryTest.M08 should fail atomically when a manifest class is missing`, `TypeRegistryTest.M08 should wrap linkage errors without publishing a registry`, `TypeManifestLoaderTest.M08 should wrap failures while enumerating manifest resources`, `DefaultStepActivationServiceTest.M08 should wrap a registered step constructor failure`, and `JobActivationServiceTest.M08 should wrap a registered job constructor failure` |
| M09 | Safe restored step | `RestoringStepActivationServiceTest.M09 restored unknown steps expose no executable actions and retain their marker` |
| M10 | Custom job alias lookup | `JobActivationServiceTest.M05 M10 should activate registered job FQCNs and aliases` |
| D01 | Repository, pagination, and FlowQuery | `AbstractProductionMongoSecurityContractIT.O02 D01 findAll pagination and FlowQuery reject an unregistered model` |
| D02 | Matching hostile model fields | `AbstractProductionMongoSecurityContractIT.R03 D02 production workflow reads reject an unregistered model before materialization` |
| D03 | Nested application values | `AbstractProductionMongoSecurityContractIT.D03 production workflow reads reject nested unregistered and wrong-role types` and `MongoDocumentTypePolicyTest.D03 D08 validates model and nested value aliases with a custom type key` |
| D04 | Step, job, and definition value fields | `AbstractProductionMongoSecurityContractIT.D04 production step reads reject types in legacy and current value fields`, `D04 production job reads reject types in legacy and current parameter fields`, `D04 production step definition reads reject nested metadata types`, and `MongoDocumentTypePolicyTest.D03 D04 infrastructure aliases are limited to framework metadata fields` |
| D05 | Legacy bootstrap and migration | `AbstractProductionMongoSecurityContractIT.D05 legacy step bootstrap rejects a type before conversion and leaves BSON unchanged`, `D05 legacy job bootstrap rejects a type before conversion and leaves BSON unchanged`, `V01 D05 legacy type-name bootstrap rejects unregistered enum and preserves BSON`, and `V03 D05 registered legacy enum migration produces safe typed records` |
| D06 | Aggregation and projection | `AbstractProductionMongoSecurityContractIT.D06 production aggregation projection rejects nested types before materialization` and `MongoDocumentTypePolicyTest.D06 projection DTOs keep VALUE role validation for nested aliases` |
| D07 | Event-independent read guard | `AbstractProductionMongoSecurityContractIT.D07 production reads stay guarded without synchronous lifecycle events` |
| D08 | Alias shape, roles, and configured key | `AbstractProductionMongoSecurityContractIT.D08 production converter rejects malformed unknown and wrong-role aliases`, `D08 aliases shared by different Mongo roles and classes fail during construction`, `AbstractProductionMongoCustomTypeKeyIT.D08 production wiring preserves and guards the configured Mongo type key`, `MongoDocumentTypePolicyTest.D08 rejects malformed and role-invalid aliases`, `MongoTypeKeyResolverTest.D08 resolves the type key actually configured on the host converter`, and `MongoTypeKeyResolverTest.D08 rejects a host converter with disabled type metadata` |
| D09 | Compatible round trips | `AbstractProductionMongoSecurityContractIT.D08 D09 production persistence accepts registered FQCN and logical aliases` and `D09 null scalar collection and map workflow models round-trip in production` |
| D10 | Type queries and rename migration | `AbstractProductionMongoSecurityContractIT.D10 type queries use only registered model subtypes`, `D10 type rename migrates the default Mongo type key before the next guarded read`, `AbstractProductionMongoCustomTypeKeyIT.D10 type queries use the configured Mongo type key`, `D10 type rename migrates the configured Mongo type key before the next guarded read`, and `RegistrySubclassProviderTest.D10 exposes only concrete registered types assignable to the query type` |
| D11 | Host converter isolation | `AbstractProductionMongoSecurityContractIT.D11 production access keeps host template converter and repository isolated` |
| D12 | Shared transaction rollback | `AbstractProductionMongoSecurityContractIT.D12 production persistence joins host transaction and rolls back` |
| D13 | Parallel access and traversal bounds | `AbstractProductionMongoSecurityContractIT.D13 parallel production accesses keep registry contexts isolated and fail closed` and the two `D13` limit tests in `MongoDocumentTypePolicyTest` |
| V01 | Legacy enum metadata | `SecurityWitnessTest.V01 enum witness stays dormant until constants are accessed`, `AbstractProductionMongoSecurityContractIT.V01 legacy value type maps reject an unregistered enum before initialization`, and `V01 D05 legacy type-name bootstrap rejects unregistered enum and preserves BSON` |
| V02 | Current typed records | `AbstractProductionMongoSecurityContractIT.V02 typed records reject an unregistered enum before initialization` |
| V03 | Registered values and fixed built-ins | `ValueTypeConverterSecurityTest.V03 registered enum accepts exact key binary and canonical registrations`, `V03 typed records restore registered enum collections date instant and null`, `V03 built in collection variants and null remain available without a registry`, `AbstractProductionMongoSecurityContractIT.V03 registered enum values round-trip through current and legacy records without TCCL lookup`, and `V03 fixed values and nested registered enums round-trip through production persistence` |
| V04 | Malformed graphs and rejected writes | `ValueTypeConverterSecurityTest.V04 unknown value type fails with its exact role and key`, `V04 missing and duplicate JVM type references fail deterministically`, `V04 cyclic and deep record graphs fail before unbounded recursion`, `V04 incompatible values and unsupported records never pass through raw`, and `AbstractProductionMongoSecurityContractIT.V04 create and save reject unregistered value types before writing` |
| V05 | Context isolation | `ValueTypeConverterSecurityTest.V05 concurrent application contexts keep their value registries isolated` |
| O01 | Reconciliation isolation | `ReconcileScheduledJobsBootstrapActionTest.O01 isolates rejected and missing jobs between healthy scheduled jobs` and `AbstractProductionMongoReconciliationIT.O01 production startup isolates an unknown payload between healthy scheduled jobs` |
| O02 | Normal queries fail visibly | `AbstractProductionMongoSecurityContractIT.O02 D01 findAll pagination and FlowQuery reject an unregistered model` |
| O03 | Read-only raw BSON audit | `AbstractProductionMongoTypeAuditIT.O03 audit reports persisted type deviations without hydration or writes`, `O03 audit accepts registered Mongo aliases and built-in value types`, and `O03 audit marks capped results incomplete and reports malformed metadata`, executed by `Boot3ProductionMongoTypeAuditIT` and `Boot4ProductionMongoTypeAuditIT` |
| O04 | WFMS-like consumer behavior | `AbstractMongoConsumerContractIT.O04 resumes an external model through a custom step query and custom job execution` and `O04 restores a persisted incompatible custom step without actions`, executed by `Boot3MongoConsumerContractIT` and `Boot4MongoConsumerContractIT` |
| O05 | Inventory and rolling-deployment boundary | `AbstractProductionMongoSecurityContractIT.O05 expand readers accept old and future models before future writes begin` and `AbstractProductionMongoTypeAuditIT.O05 audit reports inconsistent registered model metadata` |
| O06 | Complete artifact and initialized registry | `FluxFlowTypeManifestPluginTest.O06 should fail check when the jar omits the generated manifest`, `O06 should generate and verify the manifest in an executable boot jar`, `M03 O06 should reject an explicit type absent from the runtime classpath`, `FluxFlowTypeRegistryFactoryTest.O06 should publish a complete registry before its first consumer`, `O06 should fail context refresh before a consumer sees an invalid manifest`, `BasicConfigurationActivationTest.O06 should pass the context registry to job activation`, and `AbstractProductionMongoSecurityContractIT.O06 create rejects an unregistered external model before writing` |

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

O04 is an in-repository, WFMS-like compatibility fixture. It covers resume, restore, custom kinds,
queries, job execution, and an explicitly registered external model through real Spring and Mongo
wiring. It does not establish compatibility with the actual WFMS artifact, configuration, custom
conversions, or persisted data. The WFMS repository test run and the read-only inventory comparison
against an anonymized export or staging database remain required external acceptance steps.
Their status is **open** until results for the actual WFMS artifact and data shape are attached to
the release evidence.

O05 records the reason for the staged rollout: an expanded reader accepts old and future types,
while a pre-expand reader rejects a future runtime class name once it is written. The public
manifest documentation defines the corresponding Expand -> Deploy -> Migrate -> Contract sequence
and rollback boundary.

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
