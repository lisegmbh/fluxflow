# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
1. **Optional sort property**<br/>
   Enhanced Sort Functionality to Handle Nullable Properties. The Sort interface now includes a nullableProperty function that allows nullable properties to be used for sorting. [Issue #135](https://github.com/lisegmbh/fluxflow/issues/135)
2. **Inclusion of nested/recursive validation constraints**<br/>
   The Jakarta validation now reports validation constraints
   present on step data property's type, as long as the property is itself annotated with `@Valid`. [Issue #152](https://github.com/lisegmbh/fluxflow/issues/152)
3. **Versioning information for step definitions**<br />
   Step definitions and steps can now carry versioning information.
   They can be applied by annotating a step with the new `@Version` annotation.
4. **Compatibility checks and restored versions for steps**<br />
   FluxFlow can now check if the current step definition is still compatible with the version that has been used to
   originally create a step to be loaded from persistence.
   If it is incompatible, a restored/historic version of the affected step can be returned.
   See [https://docs.fluxflow.cloud/see/2](https://docs.fluxflow.cloud/see/2) for more information.
5. **Allow passing additional information when updating step data**<br />
   The `StepDataService` now accepts an additional parameter allowing developers to pass additional context information.
6. **DoesNotContainElement filter**
   The filter 'DoesNotContainElement' has been added, which matches collections not matching a given element.
7. **Not filter**<br />
   The `Not` filter has been added, which negates the result of the inner filter.
8. **Job querying feature**<br/>
   Added the ability to query for jobs using the `JobService.findAll` method. This allows developers to retrieve jobs based on various criteria.
9. **ElemMatch filter**<br />
   The `ElemMatch` filter has been added, which matches collections where the inner filter matches any element of the collection.
10. **Job duplication**
    The `duplicateJob` method has been added, which duplicates the given job and schedules it for a workflow.
11. **Workflow definition metadata**<br/>
   Similar to steps, jobs, and step data, workflow definitions now support metadata. The metadata is automatically extracted from
   annotations present on the workflow model class. This metadata can be accessed via the `WorkflowDefinition.metadata` property.
   [Issue #86](https://github.com/lisegmbh/fluxflow/issues/86) and [Issue #50](https://github.com/lisegmbh/fluxflow/issues/50)
   
### Changed
1. **Configurable replacement scope for workflow continuations**<br/>
   The `Continuation.workflow` function now supports a `replacementScope` parameter that defines which workflow elements should be replaced.
   The default value is an empty scope, which means that only the workflow itself is replaced. 

   Previously, the standard behavior of a workflow replacement continuation was to replace the steps and jobs of the workflow. [Issue #99](https://github.com/lisegmbh/fluxflow/issues/99)
2. Added an optional `version` property to MongoDB's StepDocument.
   There is no need for a migration, as the field is optional/nullable.
3. A custom collation can now be specified when ensuring indexes using `MongoBootstrapAction`.
4. MongoDB documents now use a different data structure to store map entries and their data types. See [https://docs.fluxflow.cloud/see/1](https://docs.fluxflow.cloud/see/1) for more information. 
### Deprecated
### Fixed
1. **NPE within ParamMatcher.isAssignable**<br/>
   The `NullPointerException` that has been thrown within
   `ParamMatcher.isAssignable` when passing in a generic type has been fixed.
   [Issue #158](https://github.com/lisegmbh/fluxflow/issues/158)
### Removed
1. Removed the unused `Type.toKClass()` extension function from `de.lise.fluxflow.reflection`.

## [0.1.0] - 2024-03-10
### Added
1. **Model listeners**<br/>
A model listener can be used to act upon changes to a workflow's model.
Similar to data listeners,
model listeners are declared by creating a function within the workflow model and annotating it with `@ModelListener`.
[Issue #70](https://github.com/lisegmbh/fluxflow/issues/70)
2. **Job(definition) metadata**<br/>
Jobs and job definitions now support adding metadata using annotations.
3. **Step data modification policy**<br />
A step data modification policy can be used to control the modification behaviour on inactive steps. The policy can be accessed by the step data definition.
[Issue #90](https://github.com/lisegmbh/fluxflow/issues/90)
4. **Step data metadata**<br />
Step data can now also be annotated with metadata.
   The metadata can be accessed via the step data definition. [Issue #50](https://github.com/lisegmbh/fluxflow/issues/50)

### Changed
1. Harmonized Spring Boot dependencies.
2. The Spring packages are now targeting version 3.2.1.
3. `AnyOfFilter`, `InMemoryAnyOfFilter` and `MongoAnyOfFilter` have been renamed to `[...]InFilter` and deprecated aliases have been added to maintain API compatibility. The `Filter` interface now also exposes a `Filter.in` function.
4. The `Filter.in` and `Filer.anyOf` are now accepting a more general `Collection<TModel>` parameter. Additionally, there is an overload that directly accepts the more sensible `Set<TModel>`. [Issue #101](https://github.com/lisegmbh/fluxflow/issues/101)
5. Packages depending on or extending Spring Boot functionalities are now consistently prefixed with `springboot-`. [Issue #38](https://github.com/lisegmbh/fluxflow/issues/38) 
6. The `WorkflowService` is now a composition of all CRUD-related services. [Issue #109](https://github.com/lisegmbh/fluxflow/issues/109)
   - create: `WorkflowStarterService`
   - read: `WorkflowQueryService`
   - update: `WorkflowUpdateService`
   - delete: `WorkflowRemovalService` 

### Deprecated
1. `Workflow.id` has been renamed to `Workflow.identifier`. The `id` property remains as an alias and is deprecated.
2. `AnyOfFilter`, `InMemoryAnyOfFilter` and `MongoAnyOfFilter` are now deprecated aliases for `InFilter`, `InMemoryInFilter` and `MongoInFilter`.

### Fixed
1. Changes to the step and workflow are now persisted before publishing events. [Issue #47](https://github.com/lisegmbh/fluxflow/issues/47)
2. Persisting and publishing of step and workflow updates are now skipped if the element didn't actually change. Can be disabled by setting `fluxflow.change-detection.step` or `fluxflow.change-detection.workflow` to `false`. [Issue #49](https://github.com/lisegmbh/fluxflow/issues/49)
3. Dependency injection now throws an exception if the dependency resolution fails for other reasons than a missing dependency (e.g. duplicate beans). [Issue #110](https://github.com/lisegmbh/fluxflow/issues/110)
4. Steps with custom step kinds can now be activated successfully. [Issue #115](https://github.com/lisegmbh/fluxflow/issues/115)

### Removed
1. Removed experimental and broken `de.lise.fluxflow:memorycache` module. [Issue #94](https://github.com/lisegmbh/fluxflow/issues/94)
2. The `WorkflowService.replace` function has been removed as it leaked internal functionality. [Issue #10](https://github.com/lisegmbh/fluxflow/issues/10)

## [0.0.1] - 2024-01-24

### Added
1. Initial FluxFlow release