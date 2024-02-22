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
  data:
    # boolean: If set to true,
    # step data can be modified even if the step
    # itself isn't active anymore.
    allow-inactive-modification: true
  
  change-detection:
    # boolean: If set to true, 
    # workflow persist operations are skipped if no changes could be detected. 
    workflow: true
    # boolean: if set to true,
    # step persist operations are skipped if no changes could be detected.
    step: true

  migration:
    # boolean: If set to true,
    # Quartz will automatically create and migrate its database schema.
    quartz.enabled: true

  mongo:
    # boolean: If set to false,
    # all MongoDB functionality will be disabled.
    enabled: true

  caching:
    # boolean: If set to true,
    # in-memory caching will be used. The default is only effective
    # as long as the package (`memorycache`) is on the classpath. 
    in-memory: true
```