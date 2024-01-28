# FluxFlow

[![Maven Central](https://img.shields.io/maven-central/v/de.lise.fluxflow/springboot.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22de.lise.fluxflow%22%20AND%20a:%22springboot%22)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) 
[![Build Status](https://build.lise.de/buildStatus/icon?job=Hessen+Mobil%2Ffluxflow%2Fdevelop)](https://build.lise.de/job/Hessen%20Mobil/job/fluxflow/job/develop/) 


A flexible workflow engine that helps to create and orchestrate business processes using domain code.
The fundamental idea behind FluxFlow is to provide a "lightweight"
and non-intrusive orchestrator around your domain logic, 
which will ultimately define your business process.
Unlike some BPMN engines,
the logic isn't spread across various (hard to test) artifacts like *.bpmn files but consolidated within your code.

*In a nutshell*
> With FluxFlow, your Java/Kotlin class becomes a workflow step,<br/>
> its properties hold the step's state (step data),<br />
> and its methods will define the available step actions and workflow transitions to be performed.  

FluxFlow works best with Spring Boot, providing dependency injection and support for persistence.
Nevertheless,
the core functionality doesn't depend on Spring and might also be used standalone or with any another framework.


## Further reading
- [📚 Documentation](https://docs.fluxflow.cloud)

## Sample

```kotlin
import java.time.Instant

class CreateVacationRequestStep(
    var firstname: String, // Step data
    var lastname: String,  // Step data  
    var start: Instant,    // Step data
    var end: Instant       // Step data
) {
    
    // Step action that transitions into the next step
    fun submit(): CheckVacationRequestStep {
        return CheckVacationRequestStep(
            // Step data passed into the next step
            firstname,
            lastname,
            start,
            end
        )
    }
}
```

## Features
- Lightweight workflow engine, including
  - Workflow data: Information shared during the entire workflow
  - Steps: The building blocks of workflows
  - Step data: Information specific to a step that can be fetched and update
  - Step actions: Actions and transitions that can be invoked within a step
  - Jobs: Automatic execution of logic at a specified time (requires Quartz)
  - Validation: Validation powered by the standard Jakarta Bean Validation
- Spring Boot compatible
- Allows easy (unit) testing of your workflow logic
- Extensible
- Open Source

## Getting started

### Spring Boot dependencies

**`build.gradle.kts`**
```kotlin
dependencies {
  // Base dependencies
  implementation("de.lise.fluxflow:springboot:0.0.1")
  
  // Persistence options
  // a) In-Memory: no persistence between executions; can be used for testing or experiments 
  implementation("de.lise.fluxflow:springboot-in-memory-persistence:0.0.1")
  // b) MongoDB
  implementation("de.lise.fluxflow:mongo:0.0.1")
  
  // ... your other Spring Boot dependencies
  // e.g. implementation("org.springframework.boot:spring-boot-starter-web")
}
```

## Contributors
- [Christian Scholz](https://github.com/bobmazy)
- [Dominik "Pipo" Alexander](https://github.com/DerPipo)
- [Jagadish Singh](https://github.com/jagadish-singh-lise)
- [Marcel Singer](https://github.com/masinger)
