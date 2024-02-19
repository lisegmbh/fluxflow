# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
1. **Model listeners**<br/>
A model listener can be used to act upon changes to a workflow's model.
Similar to data listeners,
model listeners are declared by creating a function within the workflow model and annotating it with `@ModelListener`.
[Issue #70](https://github.com/lisegmbh/fluxflow/issues/70)


### Changed
1. Harmonized Spring Boot dependencies.
2. The Spring packages are now targeting version 3.2.1.

### Deprecated
1. `Workflow.id` has been renamed to `Workflow.identifier`. The `id` property remains as an alias and is deprecated.

### Fixed
1. Changes to the step and workflow are now persisted before publishing events. [Issue #47](https://github.com/lisegmbh/fluxflow/issues/47)
2. Persisting and publishing of step and workflow updates are now skipped, if the element didn't actually change. Can be disabled by setting `fluxflow.change-detection.step` or `fluxflow.change-detection.workflow` to `false`. [Issue #49](https://github.com/lisegmbh/fluxflow/issues/49)

### Removed

## [0.0.1] - 2024-01-24

### Added
1. Initial FluxFlow release