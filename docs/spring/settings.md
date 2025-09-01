# Spring Settings

The following YAML shows all settings that are available when using FluxFlow with Spring Boot.
Assigned values are indicating the default values for each setting.
A setting's role and their influence is described in a comment above each key.
Note that some settings and defaults are only available and effective
if the corresponding module is present within the classpath.  

```yaml
fluxflow:
  action:
    # boolean: If set to true,
    # FluxFlow enforces all step data to be valid 
    # before actions can be invoked. 
    validate-before: true

  change-detection:
    # boolean: If set to true, 
    # workflow persist operations are skipped if no changes could be detected. 
    workflow: true
    # boolean: if set to true,
    # step persist operations are skipped if no changes could be detected.
    step: true

  data:
    # boolean: If set to true,
    # step data can be modified even if the step
    # itself isn't active anymore.
    allow-inactive-modification: true

  migration:
    # boolean: If set to true,
    # Quartz will automatically create and migrate its database schema.
    quartz.enabled: true

  mongo:
    # boolean: If set to false,
    # all MongoDB functionality will be disabled.
    enabled: true
    migrations:
      typeRecords: fail # fail = prevent startup, warn = warn about failing records
    # Can be used to tweak the default collation settings used when **initially**
    # creating FluxFlow's MongoDB collections.
    collation:
      default: # Will be used if there is no collection-specific setting.
        # The following keys represent a MongoDB collation document. Refer to
        # https://www.mongodb.com/docs/manual/reference/collation/#collation-document
        # for more information.
        locale: "" # string (required)
        strength: null # int
        caseLevel: null # boolean
        caseFirst: null # string
        numericOrdering: null # boolean
        alternate: null # string
        maxVariable: null # string
        backwards: null # boolean
        normalization: null # boolean

      # Sets the workflow document collation options.
      # Same model as `fluxflow.mongo.collation.default`, 
      # which will be used if omitted.
      workflow: null
      # Sets the step document collation options.
      # Same model as `fluxflow.mongo.collation.default`, 
      # which will be used if omitted.
      step: null
      # Sets the job document collation options.
      # Same model as `fluxflow.mongo.collation.default`, 
      # which will be used if omitted.
      job: null
      # Sets the continuationRecord document collation options.
      # Same model as `fluxflow.mongo.collation.default`, 
      # which will be used if omitted.
      continuationRecord: null
  
  quartz:
    # boolean: if set to true, Quartz will search for pre-scheduled jobs by also comparing their job data map.
    # Note that enabling this might come with a performance impact, 
    # as it may require iterating over all previously scheduled jobs [O(n^2)].
    legacyLookup: false

  scheduling:
    # boolean: if true,
    # FluxFlow will attempt to re-schedule jobs that are missing from the scheduler's persistent storage.
    reconcileOnStartup: false

  versioning:
    comparison: # Can be used to tweak the compatibility when comparing different versions
      bothUnknown: Unknown # The old and new versions are unknown
      unknownToKnown: Incompatible # The old version is unknown, while the new one is known 
      knownToUnknown: Unknown # The old version is known, but the new one isn't
      noExactMatch: Incompatible # Both versions are known, but they do not match
    steps:
      recordVersion: true # boolean: If true, step definition versions are persisted.
      requiredCompatibility: Unknown # Compatible, Unknown or Incompatible
      automaticRestore: true # If set to true, FluxFlow tries to fall back to activating
                             # failed steps using pre-persisted step definitions.
      automaticUpgrade: true # If set to true, FluxFlow 
                             # automatically updates compatible steps.
      requiredUpgradeCompatibility: Unknown # # Compatible, Unknown or Incompatible
```