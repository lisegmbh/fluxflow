# Trusted type manifest

FluxFlow artifacts can publish a versioned inventory of the application types they contribute.
Every entry has a role, an exact persisted key, and a JVM binary class name. The supported roles
are `step`, `job`, `model`, and `value`; registering a class for one role does not register it for
another.

The plugin marker is published to the same Maven repositories as the FluxFlow libraries. Add the
repository to plugin resolution in `settings.gradle.kts`; the snapshot repository is only needed
for snapshot versions:

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        maven("https://nexus.cloud.lise.de/repository/maven-public/")
        gradlePluginPortal()
    }
}
```

Apply the manifest plugin with the same version as the FluxFlow libraries:

The plugin requires Java 17 or newer and Gradle 9.2.0 or newer. Its Kotlin DSL configuration
and manifest generation are tested on Gradle 9.2.0 and the repository's Gradle 9.8.0 wrapper.

```kotlin
plugins {
    id("de.lise.fluxflow.type-manifest") version "<fluxflow-version>"
}
```

The plugin finds `@Step` and `@Job` classes in the project's compiled output. It inspects them
without running static initializers and writes the resulting inventory to
`META-INF/fluxflow/type-manifest.properties`. The generated resource is part of the main runtime
classpath and is stored at the same path in plain and executable Spring Boot JARs. Generation
also records `manifest.covered-packages` for the compiled packages of that module. Explicit
types must be available on the runtime classpath; a `compileOnly` declaration fails manifest
generation.

Annotation discovery reads bytecode, so unrelated classes whose supertypes are available only
through `compileOnly` do not prevent generation. Registered types and their supertypes still
must be resolvable from the application's runtime classpath. Gradle's own classes do not satisfy
this requirement.

For IntelliJ IDEA runs, delegate build and run actions to Gradle. The generated manifest is a
Gradle task output; IntelliJ's native compiler does not regenerate it after an annotated class
is renamed, removed, or assigned another kind. A new or moved `@Step`/`@Job` class in a package
listed by `manifest.covered-packages` is also missing from the registry until
`generateFluxflowTypeManifest` runs. If a native IDE build reports a stale-manifest conflict or
a missing kind, run that task before restarting the application.

Types without a FluxFlow annotation must be declared explicitly. This preserves support for
plain domain classes while keeping their registration visible in code review:

```kotlin
fluxflowTypeManifest {
    step(
        "com.example.workflow.SubmitOrderStep",
        "com.example.workflow.SubmitOrderStep",
    )
    model("order", "com.example.workflow.OrderModel")
    value("currency", "com.example.workflow.Currency")
}
```

The first argument is the persisted key and the second is the JVM binary class name. For a nested
class, the binary name contains `$`, for example `com.example.Order$Line`. Legacy fully qualified
keys remain valid when they are declared explicitly; FluxFlow does not try alternate package,
case, whitespace, or nested-class spellings.

Spring applications may also contribute a compiler-checked, context-local registration. This is
useful when the build belongs to another artifact or when a class is selected by application
configuration:

```kotlin
@Bean
fun workflowTypes() = TypeRegistrationContributor {
    listOf(
        TypeRegistration(TypeRole.MODEL, "order", OrderModel::class),
        TypeRegistration(TypeRole.VALUE, "currency", Currency::class),
    )
}
```

At startup, FluxFlow loads all manifests visible to the application class loader, adds annotated
application types from packages that no loaded manifest covers, and explicit contributors, and
then publishes one immutable registry for that application context. Packages listed in
`manifest.covered-packages` are not scanned again; their inventory comes from the manifest and
from contributor beans. Manifests without that property keep the previous full scan. Identical
registrations are deduplicated. A malformed resource, missing class, or conflicting role/key
mapping stops startup and reports both origins.

Step and job activation resolve persisted kinds exclusively through this registry. An exact entry
with the matching role must exist before FluxFlow initializes or constructs the declared class.
Unknown kinds fail with a `StepActivationException` or `JobActivationException`; the cause is an
`UnknownTypeException` that identifies the rejected role and key. Scheduling a job and creating a
step require that same entry before anything is written. An unknown job kind fails with
`JobActivationException` and the message `Unable to schedule job with kind '<kind>'`, without
cancelling, persisting, or scheduling. An unknown step kind fails with `StepActivationException`
before the step document or its definition snapshot is written. Reading jobs stays
fail-closed: one job that cannot be activated fails the whole `findAllJobs` call. Mongo model and value type
metadata use the same inventory in a separate hardening step.
This startup validation takes effect immediately when upgrading, including for `model` and
`value` registrations before their persistence consumers use the registry exclusively.
For example, two discovered `@Job("notify")` classes now prevent startup, as does
a dependency manifest referencing a class absent at runtime. Audit duplicate kinds and dependency
manifests before upgrading; invalid registrations are not ignored.

Registry discovery and name resolution use the configured `ClassLoaderProvider`, matching step
resolution. Contributors retain the exact `KClass` they supply. Registrations for the same
role/key and binary name from different class loaders conflict if they identify different JVM
classes. Contributor classes must also be compatible with the application's shared APIs.

Before upgrading a running system, compare the distinct persisted step and job kinds with the
generated inventory. Every historical kind that can still be activated needs an exact `step` or
`job` declaration. Compare model/value type metadata as preparation for Mongo hardening as well.
Database contents may identify missing declarations, but must never add registrations
automatically. Applications with dynamic or erased model types need explicit entries. A repository
fixture cannot replace an inventory check against the actual application's data.

Mongo reads, type-record bootstrap migration and scheduled-job reference queries use the host
converter's mapping context, including custom field names and status converters. Mapped PATH fields
follow their BSON nesting; literal KEY field names retain their dots. Infrastructure mapping namespaces do not
authorize type metadata on themselves or on unknown neighboring fields. `isType` queries
accept registered logical MODEL and VALUE aliases as well as their JVM binary names. An isolated
projection of a class registered in both roles accepts either declared alias at its root;
nested values still require VALUE registration and full workflow models require MODEL registration.

### Internal Mongo customization contract

FluxFlow constructs a non-bean `MongoTemplate` for its own persistence. Its converter keeps the
host `MappingContext` and custom conversions, but it is **not** a general-purpose clone of the host
`MappingMongoConverter`: its type mapper is replaced by the mandatory guarded mapper. The configured
host type key is discovered behaviorally and retained. Spring Data `@TypeAlias` values are not an
authorization source; a MODEL or VALUE alias is accepted only when the immutable FluxFlow manifest
registers that exact alias (the JVM binary name remains read-compatible when registered).

Applications may provide ordered `FluxFlowMongoConverterCustomizer` beans. They can explicitly set
map-key dot replacement, preserve dotted map keys, or replace `EntityCallbacks` (including
`AfterConvertCallback`) for the internal converter. The customizer intentionally cannot access the
converter, mapping context, or type mapper. Therefore it cannot weaken the synchronous document guard.
Map-key handling is never inferred from the host converter: without an explicit customizer, FluxFlow
uses Spring Data's strict default and rejects dotted keys on its own writes. Configure the same policy
explicitly when workflow data contains such keys.

The internal converter and template receive the application context, so their callback and mapping-event
behavior is context-local. The repository factory receives that context's `Environment` and, when exactly
one is present, its `ProjectionFactory`; multiple `ProjectionFactory` beans fail startup rather than being
chosen arbitrarily. The host mapping context is shared read-only for mapping metadata. FluxFlow does not
create, initialize, or alter a second mapping context, publish host mapping events, or trigger host auto-index
creation. Template-only options remain available through ordered `FluxFlowMongoTemplateCustomizer` beans.

Scheduled-job reconciliation uses raw references when the persistence implementation supplies
`ScheduledJobReferencePersistence`, then restores each job independently. The deprecated
two-argument `ReconcileScheduledJobsBootstrapAction(JobService, SchedulingService)` constructor
and `BasicConfiguration.startupJobReconciliation(JobService, SchedulingService)` factory remain
available for existing callers. They retain bulk reads and fail-fast behavior, including guarded
type errors, because they do not receive raw references or a `WorkflowService`.
The production Spring bean uses the four-argument factory. Subclasses that customized the old
two-argument factory must move that override to the four-argument factory to customize this bean.

The manifest is an authorization inventory. It does not provide cryptographic integrity for an
artifact or validate the constructor arguments and data belonging to an allowed type.
