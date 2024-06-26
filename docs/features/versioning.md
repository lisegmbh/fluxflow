# Versioning

As a workflow's behavior and its associated data is defined by your code,
this might require the need to handle incompatible code changes.
Depending on the reason for said change, developers might mitigate them in one of the following ways:

1. **Bugfixes**: If the change has been made to fix a previously occurring bug
   and doesn't alter the business behavior,
   one would usually fix this by doing a migration.
2. **Refactorings**: This type of change usually arises when workflow defining elements are renamed, moved,
   or modified in any other way that is preserving the original business logic.
   This too can be solved by creating and executing a migration.
3. **Changes to the business logic**: If a new code version becomes incompatible because the fundamental business logic
   has been altered, a migration of existing or completed workflows might not be suitable.
   This is because it would "alter the past" and suggest that a workflow has been carried out in a way it actually
   wasn't.

All these cases and their mitigation strategies
require developers to be able to declare and obtain a workflow element's version.

!!! warning "Work in progress"

    Please note that the feature complex around versioning and migrations
    is currently in active development.
    Expect new featues, changes and some limitations:

    + Versioning is currently only available for steps

## Introduction

### General rules and concepts

When working with workflow elements supporting versioning, the following general rules and principles apply.

1. Every supported workflow element definition has an associated version, reflecting its current state.
2. If the developer didn't declare a specific version, an "empty version" is assumed.
3. When creating a new workflow element based on that definition,
   the current definition version will be stored alongside the created workflow element.
4. Once a workflow element has been created, its version won't change.
5. When activating the workflow element, FluxFlow will check if the persisted version is compatible with the current
   definition version.
6. If it isn't, FluxFlow will assume that the element is historic
   and tries to return a special read-only view of that element.

### Version

In this context,
a version is defined as an arbitrary string,
used to distinguish between various states of a workflow elements definition.
As such, it is up to the developer to use any desired versioning scheme
(e.g., SemVer, GUIDs, hashes, code names, and so on).

When comparing versions, FluxFlow uses the following logic to evaluate if they are compatible.

| Baseline version | Compared version                  | Compatibility  |
|------------------|-----------------------------------|----------------|
| No/Empty version | Any other version                 | `Unknown`      |
| Any version      | No/Empty version                  | `Unknown`      |
| Any version      | Any exactly matching version      | `Compatible`   |
| Any version      | Any version, not matching exactly | `Incompatible` |

!!! warning "Work in progress"

    Currently there is no further interpretation of a given version string.

    Be aware, that it is planned to natively support the compatibility expressed 
    by semantic versions.


## Usage

### Specifying a version
A workflow elements version can be specified using the `@Version` annotation.

```kotlin
import de.lise.fluxflow.stereotyped.step.Step
import de.lise.fluxflow.stereotyped.versioning.Version

@Step
@Version("0.0.5")
class EnterContactInformationStep {
    // [...]
}
```

To explicitly specify older but still compatible versions,
the `compatibleVersions` property can be used. 

```kotlin
@Step
@Version(
    "0.0.5",
    compatibleVersions = ["0.0.4", "0.0.3"]    
)
class EnterContactInformationStep {
    // [...]
}
```

### Getting a step definition's version
A definition's version is exposed by the `version` property on the API object.

```kotlin
fun logDefinitionVersion(stepDef: StepDefinition) {
    Logger.debug("Found step definition {} with version {}", stepDef.kind, stepDef.version)
}
```

### Tweaking the required compatibility
By default, FluxFlow will require versions to at least be not incompatible.
In other words, the compatibility check must evaluate to `Compatible` or `Unknown`.

This default behavior can be tweaked using the 

`fluxflow.versioning.<elementType>.requiredCompatibility` 

setting.

Assigning `Incompatible` will effectively disable the version compatibility check,
while `Compatible` requires the versions to be explicitly compatible.

```yaml
# disable version compatibility for steps
fluxflow.versioning.steps.requiredCompatibility: Incompatible
---
# require explicit compatibility
fluxflow.versioning.steps.requiredCompatibility: Compatible
```

### Tweaking the fallback behavior
Whenever FluxFlow is unable to reactivate a workflow element, 
it will try to return a "historic" fallback representation using a snapshot of its original definition.
Note that the activation might fail because the compatibility check failed 
or because of an actual runtime error during activation
(e.g., missing class definitions, incompatible data types, etc.). 

The restored representation is immutable 
and its purpose is to still be able to at least access historic information.

Restored steps can be detected by checking whether it implements the `de.lise.fluxflow.api.step.RestoredStep` interface.

This fallback behavior can be disabled by setting

`fluxflow.versioning.<elementType>.automaticRestore`

to `false`.
In which case a `<elementType>ActivationException` (e.g. `StepActivationException`) is going to be thrown.


```yaml
fluxflow.versioning.steps.automaticRestore: false
```
