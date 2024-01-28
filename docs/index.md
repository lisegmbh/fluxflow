# FluxFlow

FluxFlow is a flexible workflow engine that helps to create and orchestrate business processes using domain code.
The fundamental idea behind FluxFlow is to provide a "lightweight" and non-intrusive orchestrator around your domain logic,
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