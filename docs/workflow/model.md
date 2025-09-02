# Workflow models

A workflow model holds the information, which is shared during the entire workflow execution.

## Definition

To define a workflow model, creating a plain old Java/Kotlin class is enough.

```kotlin
import java.time.Instant

enum class Status {
    Draft,
    Submitted,
    Approved,
    Rejected
}

data class VacationRequestWorkflow( // This will be the workflow model.
    val creationTime: Instant,
    var status: Status = Status.Draft
)
```

### Adding metadata to the model definitions
The workflow model can also carry metadata. 
Workflow model metadata is defined using annotations that are themselves meta-annotated with `@Metadata`.

```kotlin
import de.lise.fluxflow.stereotyped.metadata.Metadata

@Metadata("displayName")
annotation class DisplayName(val value: String)

@Metadata("description")
annotation class Description(val value: String)

@DisplayName("Vacation Request")
@Description("A workflow model for managing vacation requests")
data class VacationRequest(
    val creationTime: Instant,
    var status: Status = Status.Draft
)
```

Workflow metadata is exposed through `Workflow.definition.metadata`. 
This provides a key-value map containing all metadata entries.

## Usage

Using the workflow model is as easy as passing an instance into the `WorkflowStarterService.start` function.
The created workflow will from now on use that model as its workflow model.

```kotlin
@RestController
@RequestMapping("/api/vacationrequest")
class VacationRequestController(
    private val workflowStarterService: WorkflowStarterService
) {
    @PostMapping
    fun createRequest() {
        workflowStarterService.start(
            VacationRequestWorkflow( // Create the workflow model
                Instant.now() // while passing in its initial state
            ),
            Continuation.none() // replace this with your actual steps
        )
    }
}
```

## Workflow actions
Workflow actions are similar to [step actions](../steps.md/#actions).  
They are used to execute custom logic and influence the workflow’s control flow.  
Unlike step actions, a workflow action is defined in the context of the entire workflow.  
It does not require an active workflow step
and is available throughout the workflow’s lifetime.

For more general information about actions and their purpose,  
see the section on [step actions](../steps.md#actions).

### Defining a workflow action
Workflow actions are defined by creating a function annotated with `@Action`.
In contrast to step actions, a workflow action always requires this annotation in order to be detected.

```kotlin
data class VacationRequestWorkflow(
    val creationTime: Instant,
    var status: Status = Status.Draft
) {
    @Action
    fun export() {
        // some logic that can/should be executed independently within the workflow's context
    }

    fun isOlderThan10Days(): Boolean {
        // this is a regular function, having no meaning to FluxFlow
        return Duration.between(creationTime, Instant.now()) > Duration.ofDays(10)
    }
}
```
Workflow actions support the same features as step actions, 
which are also applied in the same way (e.g. [continuations](../steps.md#continuing-the-workflow-using-return-values)
or [dependency injection](../steps.md#injection-into-action-functions)).
Please refer to the corresponding section.

### Obtaining and executing a workflow action
A workflow action can be executed using the `WorkflowActionService`.
Simply invoking the `WorkflowAction.execute()` function would execute the action's payload,
but fail to handle any side effects like persisting the new workflow state or executing any continuation.

```kotlin
@Service
class ExportService(
    private val workflowService: WorkflowQueryService,
    private val workflowActionService: WorkflowActionService
) {
    fun triggerExport(
        workflowIdentifier: WorkflowIdentifier
    ) {
        val workflow = workflowService.get<VacationRequestWorkflow>(workflowIdentifier)
        workflowActionService.invokeAction(
            workflow,
            ActionKind("export")
        )
    }
}
```

## Listening for changes (model listeners)

Sometimes it is necessary to react to changes to a workflow's model.
The most straight forward approach would be to have this logic right next to the actor that causes these changes.
If this is not suitable, or if you want to react to changes in a centralized way,
the concept of model listeners can be used.

A model listener is a special function within a workflow's model.
It will be invoked whenever the model changes in a way, that is relevant to the listener.

### Listening for any changes

The most basic listener will be invoked whenever anything within the model changes (= the new model state is not equal
to the old one).

It can be defined by declaring a public function with the workflow model's type and annotating it with `@ModelListener`.

```kotlin
data class VacationRequestWorkflow(
    val creationTime: Instant,
    var status: Status = Status.Draft
) {
    @ModelListener
    fun anythingChanged() {
        // react to changes
    }
}
```

If you need to access the workflow API object, you can simply declare an appropriately typed parameter.
The same is true if your code needs to use types from dependency injection.

```kotlin
@Service
class MailService {
    fun sendMail() { /* Implement */
    }
}

data class VacationRequestWorkflow(
    val creationTime: Instant,
    var status: Status = Status.Draft
) {
    @ModelListener
    fun anythingChanged(
        // receives the currently executing workflow
        currentWorkflow: Workflow<VacationRequestWorkflow>,
        // receives the service from dependency injection
        mailService: MailService
    ) {
        // react to changes
    }
}
```

The final type of parameters a model listener might declare,
are receivers for the original/unmodified and current state.
This is done by creating a parameter of the workflow models type.

```kotlin
data class VacationRequestWorkflow(
    val creationTime: Instant,
    var status: Status = Status.Draft
) {
    @ModelListener
    fun anythingChanged(
        // receives the unmodified state
        oldModel: VacationRequestWorkflow,
        // receives the current state
        newModel: VacationRequestWorkflow
    ) {
        // react to changes
    }
}
```

### Listening for certain changes

Sometimes it is desirable to only invoke a listener if a certain aspect of the model has changed.
The problem is best illustrated by the following example:

```kotlin
data class VacationRequestWorkflow(
    val creationTime: Instant,
    var status: Status = Status.Draft
) {
    @ModelListener
    fun logStatusChanged(
        oldModel: VacationRequestWorkflow,
        newModel: VacationRequestWorkflow
    ) {
        if (oldModel.status == newModel.status) {
            return // The status didn't really change
        }
        LoggerFactory
            .getLogger(VacationRequestWorkflow::class.java)
            .info("New status: {}", status)
    }
}
```

The problem can be solved by specifying a `selector` within the `@ModelListener` annotation.
A selector is
a [Spring Expression Language](https://docs.spring.io/spring-framework/docs/3.2.x/spring-framework-reference/html/expressions.html#expressions-language-ref)
expression,
which will automatically be invoked for the old and new model state.
The results are then compared with each other in order to decide if the listener must be invoked.

```kotlin
data class VacationRequestWorkflow(
    val creationTime: Instant,
    var status: Status = Status.Draft
) {
    // only invoked, if `.status` changed between the old and new state
    @ModelListener("#root.status")
    fun logStatusChanged() {
        LoggerFactory
            .getLogger(VacationRequestWorkflow::class.java)
            .info("New status: {}", status)
    }
}
```

It is also possible to do the entire decision within the selector.
Meaning that the listener will be invoked whenever it evaluates to `true`.
To do so, set `selectorReturnsDecision` to `true` and do the comparison within the selector.

```kotlin
data class VacationRequestWorkflow(
    val creationTime: Instant,
    var status: Status = Status.Draft
) {
    // only invoked, if `.status` changed between the old and new state
    // and the new value is Approved
    @ModelListener(
        "#new.status != #old.status and #new.status.name == T(Status).Approved'",
        selectorReturnsDecision = true
    )
    fun logStatusChanged() {
        LoggerFactory
            .getLogger(VacationRequestWorkflow::class.java)
            .info("Vacation request has been approved")
    }
}
```

During selector evaluation,
the following variables can be used to select the relevant model information or perform the comparison itself.

| Variable | Synonyms    | Description                                                                                           |
|---------:|-------------|-------------------------------------------------------------------------------------------------------|
|  `#root` | `#current`  | Holds the model instance to select from.<br/>Useful when `selectorReturnsDecision` is set to `false`. |
|   `#old` | `#original` | Holds the old model state.<br/>Useful when `selectorReturnsDecision` is set to `true`.                |
|   `#new` | `#updated`  | Holds the new model state.<br/>Useful when `selectorReturnsDecision` is set to `true`.                |
