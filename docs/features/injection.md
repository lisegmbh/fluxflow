# Injection
To be able to access information about the currently executing workflow
external functionalities, FluxFlow provides various injection points.

## Injection targets

FluxFlow only supports direct parameter injection. The parameter might
be a constructor or method parameter. Other injection mechanisms (like
field injection, property/setter injection) are not supported.

In order for such parameters to be able to receive injected values, they
must be part of a method or constructor that will be invoked by
FluxFlow.

### Steps

Every step definition is eligible to receive injected values using their
constructor parameters.

**Receiving dependencies using a step’s constructor parameters**

    @Step
    class CheckVacationRequestStep(
        calendarService: CalendarService,
        private val mailService: MailService
    ) {
        // ...
    }

All values that need to be injected are resolved and evaluated every
time the constructor needs to be invoked by FluxFlow. This is usually
the case, whenever the step needs to be activated because it is queried
or another workflow action associated with that step is about to be
executed.

Be aware that FluxFlow also restores the previous step data state in a
similar manner, by either passing the persisted values into matching
constructor parameters or calling the relevant properties or setters.

Whenever it is ambiguous whether a constructor parameter refers to a
step data value or an external dependency, the step data will take
precedence.

To avoid the performance overhead caused by the resolution of
conditionally required dependencies, it is usually a good idea to inject
them into a more specific injection target (see [Workflow elements
represented by methods](#injection_targets_methods)).

### Workflow elements represented by methods

Workflow elements that are modeled by declaring a certain method are all
valid injection targets. Such workflow objects elements are:

1.  [Actions](#actions)

2.  [Data listeners](#step_data_listen_for_changes)

3.  [Automation functions](./automation.md#injection-into-automation-functions)

4.  [Job payload functions](#_payload)

**Receiving dependencies using various method parameters**

    @Step
    class CheckVacationRequestStep(
        @Data
        var startOfVacation: Instant
    ) {

        @Action
        fun approve(
            mailService: MailService
        ) {
            // ...
        }

        @DataListener("startOfVacation")
        fun onStartOfVacationChanged(
            newValue: Instant,
            mailService: MailService
        ) {
            // ....
        }

    }

If the executing method wants to continue the workflow with a new
workflow element - which itself requires some dependencies during
instantiation - you can declare those transitive dependencies as a
regular parameter and pass the value along.

## Injection sources

### Workflow context

In order to receive information about the currently executing workflow,
most injection targets can declare dependencies on the corresponding API
objects.

#### Step

If a workflow element (such as an action, data listener or automation
function) wants to obtain information on the step that itself belongs
to, it may declare a `Step` parameter.

**Requesting the currently executing workflow step**

    @Step
    class CheckVacationRequestStep(
        @Data
        var startOfVacation: Instant
    ) {
        @Action
        fun approve(
            currentStep: de.lise.fluxflow.api.step.Step
        ) {
            // ...
        }
    }

Do not confuse the API model `de.lise.fluxflow.api.step.Step` with the
`@Step` annotation (`de.lise.fluxflow.stereotyped.step.Step`).

If the workflow element is only interested in the step’s identifier
(e.g. for logging), it can also be requested directly by declaring a
`StepIdentifier` parameter.

**Only requesting the step’s identifier**

    @Step
    class CheckVacationRequestStep(
        @Data
        var startOfVacation: Instant
    ) {
        @Action
        fun approve(
            currentStepId: StepIdentifier
        ) {
            // ...
        }
    }

#### Workflow

Workflow elements can also request the associated workflow, by declaring
a `Workflow<>` parameter. This injection source is available for all
workflow elements.

**Requesting the currently executing workflow**

    @Step
    class CheckVacationRequestStep {

        @OnCreated
        fun sendNotification(
            workflow: Workflow<VacationRequest>, // 
        ) {
            // 
            notificationService.sendNewApprovalRequiredNotification(workflow.model.approvingManager)
        }

    }

-   This parameter will receive the currently executing workflow.

-   The `notificationService` is only here to demonstrate how one could
    use the injected workflow’s model to access further information.

Because of the JVM type erasure, the `Workflow<TModel>` 's generic type
`TModel` will not be available at runtime. You need to make sure, that
the requested type indeed matches the workflow’s model. Otherwise, a
`ClassCastException` might be thrown upon invocation.

This is also why the workflow’s model cannot be injected directly.

If the workflow element is only interested in the workflow’s identifier,
it can also be requested directly be declaring a `WorkflowIdentifier`
parameter.

**Only requesting the workflow’s identifier**

    @Job
    class SendVacationRequestReminderJob {
        private val log = LoggerFactory.getLogger(SendVacationRequestReminderJob::class.java)!!

        fun sendReminder(
            workflowIdentifier: WorkflowIdentifier
        ) {
            log.info("Sending reminder for workflow {}", workflowIdentifier)
        }
    }

#### Job

A Job’s payload function is additionally able to request the API object
describing the currently executing job by defining a `Job` parameter.

**Requesting the currently executing job**

    @Job
    class SendVacationRequestReminderJob {

        fun sendReminder(
            job: de.lise.fluxflow.api.job.Job
        ) {
            // ...
        }

    }

Be sure not to confuse the correct `de.lise.fluxflow.api.job.Job` with
any other `Job` type.

As with all identifier API objects, it’s also possible only to request
the job’s identifier. This is archived by defining a `JobIdentifier`
parameter.

**Only requesting the job’s identifier**

    @Job
    class SendVacationRequestReminderJob {

        fun sendReminder(
            jobIdentifier: JobIdentifier
        ) {
            // ...
        }

    }

### Configuration values and expressions

If a workflow element needs to access values from the current
configuration or wants to evaluate custom expressions, Spring’s `@Value`
annotation can be used.

**Requesting a configuration value**

    @Step
    class CheckVacationRequestStep {

        @OnCreated
        fun sendNotification(
            workflow: Workflow<VacationRequest>,
            @Value("\${notifications.vacationRequest.created:true}")
            notificationsEnabled: Boolean,
            notificationService: NotificationService,
        ) {
            if(!notificationsEnabled) {
                return
            }
            notificationService.sendNewApprovalRequiredNotification(workflow.model.approvingManager)
        }

    }

More information regarding the `@Value` annotation and its features can
be found within [the Spring
Documentation](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/value-annotations.html).

### IoC Container

Last but not least, FluxFlow supports requesting dependencies from the
inversion of control container. In effect, all registered beans are
available to be injected into workflow elements.

The dependency resolution is currently limited to evaluating the
parameter’s formal type. Controlling the dependency resolution using the
bean’s name or any other Spring Boot annotation is currently not
supported.
