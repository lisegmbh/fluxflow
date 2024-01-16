# Overview

In FluxFlow, jobs play a crucial role in asynchronously performing tasks
within a workflow at specified times. Jobs enable the parallel execution
of tasks, facilitate distributed processing, and offer flexibility in
managing complex workflows.

A job represents an independent unit of work executed asynchronously
within a workflow. It encapsulates specific tasks or workloads to be
performed at designated times. Jobs can be scheduled and rescheduled,
allowing for automation of periodic tasks and timely execution.

Interacting with other workflow components, jobs can exchange data,
update the workflow state, generate notifications, and initiate further
actions.

Jobs enhance workflow execution by providing asynchronous task
execution, flexibility, and automation. This section will explore job
configuration, scheduling, interaction with workflow components, and
best practices for incorporating jobs into workflow designs using
FluxFlow.

# Usage

## Job definition

A job’s definition is responsible for defining a job’s behavior. How a
job can be declared, is lined out within this section.

### Payload

A job’s behavior can be defined by implementing a new class that is
annotated with `@Job`.

**Example job definition**

    @Job
    class SendReminderJob {
        fun doSendMail() {
            // Do work here
        }
    }

The `doSendMail` method (in this example) holds the functionality that
is going to be executed, once the scheduled time has come. It doesn’t
matter how the method is named. As long as the following conditions hold
true, FluxFlow will be able to discover and use it:

1.  the method is public

2.  the method is not abstract

3.  the method is the only public method within its type or is annotated
    with `@JobPayload`

4.  all declared parameters (if present), only refer to

    1.  an object that can be obtained from the IoC container

    2.  the `Workflow<*>` API object

    3.  the `Job` API object

### Parameters

Most of the time, we need a way to dynamically customize a job’s
behavior. Sticking to the previous example, we might want to specify the
mail receiver during job creation.

This functionality is provided by "job parameters". A job parameter is
declared by creating a public property.

**Job definition accepting parameters**

    @Job
    class SendReminderJob(
        val receiverAddress: String // 
    ) {
        fun doSendMail() {
            // Do work here
        }
    }

-   Note, that the property is public

When a job is scheduled, FluxFlow will inspect the assigned property
values and persist them for later use. As soon as the job is about to be
executed, the values will be restored from the database and passed into
the job.

Contrary to step data, job parameters can not be updated after the
owning job has been scheduled.

In order for FluxFlow to be able to detect and process job parameters,
the following conditions have to be met.

1.  the property must be public

2.  the property value’s type must be serializable

During job activation, the parameter values are passed into the job’s
constructor. Each constructor parameter is matched to the originating
job parameter using its name. Therefore, it is utterly important to have
the property and constructor parameter named identically.

FluxFlow currently only supports constructor initialization for job
parameters. Setter-based injection is therefore unavailable.

### Constructor injection

In order to access external functionality from within a job, constructor
injection can be used to receive certain objects. During job
construction FluxFlow tries to resolve a value for each declared
constructor parameter. The priority and logic of said resolution is
given by the table
"[table\_title](#jobs_usage_constructor_injection_priorities)".

<table>
<caption>Job constructor injection priorities</caption>
<colgroup>
<col style="width: 16%" />
<col style="width: 16%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p><strong>Priority</strong></p></td>
<td style="text-align: left;"><p>Source</p></td>
<td style="text-align: left;"><p>Description</p></td>
<td style="text-align: left;"><p>Prerequisites</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><strong>0</strong></p></td>
<td style="text-align: left;"><p>Workflow model</p></td>
<td style="text-align: left;"><p>The workflow model associated with the
job’s workflow.</p></td>
<td style="text-align: left;"><p>The workflow model’s type must be
assignable to the parameter’s type.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><strong>1</strong></p></td>
<td style="text-align: left;"><p>Job parameters</p></td>
<td style="text-align: left;"><p>The parameter’s as they have been set
during the job’s scheduling.</p></td>
<td style="text-align: left;"><ol type="1">
<li><p>The job parameter’s type must be assignable to the constructor
parameter’s type</p></li>
<li><p>The constructor parameter name must match the job parameters name
(which is obtained from the associated property)</p></li>
</ol>
<p>See <a href="#job_usage_definition_parameters">Parameters</a> for
more information.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><strong>2</strong></p></td>
<td style="text-align: left;"><p>IoC container</p></td>
<td style="text-align: left;"><p>FluxFlow tries to obtain an instance of
the requested type from the inversion of control container (e.g. a
Spring Bean)</p></td>
<td style="text-align: left;"><ol type="1">
<li><p>There is an IoC container</p></li>
<li><p>The IoC container is able to provide an instance that is
assignable to the parameter’s type</p></li>
</ol></td>
</tr>
</tbody>
</table>

Job constructor injection priorities

A typical use case for this functionality is to inject services, which
will do the heavy-lifting and can be shared among different kind of
jobs. In our example this might be a service that provides the actual
mail sending capabilities. Using this approach, we can avoid having to
reimplement that functionality for each job wich might be sending a mail
notification.

**Example on how to inject external functionality using constructor
injection**

    @Service // 
    class MailService {
        fun sendMail(receiver: String) {
            // send actual mail
        }
    }

    @Job
    class SendReminderJob(
        val receiverAddress: String, // 
        private val mailService: MailService // 
    ) {
        fun doSendMail() {
            mailService.sendMail(receiverAddress)
        }
    }

-   The job declares a private primary constructor property of type
    `MailService`. As the resulting property is not public FluxFlow will
    not try to resolve the value using a job parameter.

-   Contrary to that, the `receiverAddress` will be obtained from the
    job’s parameters, as there is a public property with a matching name
    (due to the constructor parameter being declared as `val` without a
    `private` or `protected` modifier).

-   The dependency to be fetched from inversion of control container
    must have been registered. In this example we use Spring and
    registered the service with the `@Service` annotation.

### Payload function injection

As already mention in "[???](#payload_function_requirements)", a payload
function can declare parameters. Similar to the [Constructor
injection](#job_usage_constructor_injection), this can be useful to
access external functionality or to obtain information regarding the
current execution. Parameter resolution is done as outlined in the table

<table>
<caption>Payload function injection priorities</caption>
<colgroup>
<col style="width: 16%" />
<col style="width: 16%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p><strong>Priority</strong></p></td>
<td style="text-align: left;"><p>Source</p></td>
<td style="text-align: left;"><p>Description</p></td>
<td style="text-align: left;"><p>Prerequisites</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><strong>0</strong></p></td>
<td style="text-align: left;"><p><code>Job</code> API object.</p></td>
<td style="text-align: left;"><p>The <code>Job</code> API object,
representing the currently executing job.</p></td>
<td style="text-align: left;"><p>The parameter’s type must be assignable
from <code>Job</code>.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p><strong>1</strong></p></td>
<td style="text-align: left;"><p><code>Workflow&lt;TModel&gt;</code> API
object</p></td>
<td style="text-align: left;"><p>The <code>Workflow&lt;TModel&gt;</code>
API object, representing the currently executing job’s
workflow.</p></td>
<td style="text-align: left;"><ol type="1">
<li><p>The parameter’s type must be assignable from
<code>Worfklow</code>.</p></li>
</ol></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p><strong>2</strong></p></td>
<td style="text-align: left;"><p>IoC container</p></td>
<td style="text-align: left;"><p>FluxFlow tries to obtain an instance of
the requested type from the inversion of control container (e.g. a
Spring Bean)</p></td>
<td style="text-align: left;"><ol type="1">
<li><p>There is an IoC container</p></li>
<li><p>The IoC container is able to provide an instance that is
assignable to the parameter’s type</p></li>
</ol></td>
</tr>
</tbody>
</table>

Payload function injection priorities

The direct injection of a workflow’s model is currently unsupported due
to technical limitations. Inject the `Workflow<TModel>` instead and
access its model using the `.model` property.

It is recommended to use [constructor
injection](#job_usage_constructor_injection) for a job’s general
prerequisites (e.g. dependencies vital to a job’s functionality that are
unspecific to the workflow) and [function
injection](#job_usage_payload_function_injection) for dependencies that
are related to the currently executing workflow.

Sticking to the previous example, we can now also send the notification
to other "workflow observers".

**Example of using payload function injecting to access the current job
and workflow**

    class VacationRequest(
        val otherObservers: List<String>
    )

    @Job
    class SendReminderJob(
        val receiverAddress: String,
        private val mailService: MailService
    ) {
        fun doSendMail(
            job: Job,
            workflow: Workflow<VacationRequest>
        ) {
            System.out.println("Executing job: " + job.identifier)
            mailService.sendMail(receiverAddress)
            workflow.model.otherObservers.forEach{ observerAddress ->
                mailService.sendMail(receiver)
            }
        }
    }

### State changes

A job might change the workflow state and data of its owning workflow.
All changes applied to it will be persisted after the job has been
successfully run. If an exception occurrs, the changes will not be
committed and instead be rolled back.

**Example of a job modifying its workflow’s data**

    class VacationRequest(
        val otherObservers: List<String>,
        var notificationSent: Boolean = false
    )

    @Job
    class SendReminderJob(
        val receiverAddress: String,
        private val mailService: MailService
    ) {
        fun doSendMail(
            job: Job,
            workflow: Workflow<VacationRequest> // 
        ) {
            // send actual mail
            workflow.model.notificationSent = true // 
        }
    }

-   Inject the owning workflow using payload function injection

-   Accessing and modifying its data

## Scheduling

As far as FluxFlow is concerned, all jobs are scheduled for a fixed and
absolute time. This has been an intentional design decision, which aims
to reduce the scheduling complexity while allowing the developers to
create scheduling logic based on the application’s domain logic.

### Using a continuation

FluxFlow provides the `JobContinuation` which indicates to the workflow
engine, that a new job should be scheduled. As with every kind of
`Continuation`, there are multiple ways to request their execution.
Those are described in detail within the next sections.

In order to construct a `JobContinuation` the `Continuation.job(...)`
function should be utilized. This function expects two parameters, the
first one being the time the job should be executed, while the second
parameter specifies the actual job to be executed. The job passed into
the function is usually an instance of a job definition as described in
"[Job definition](#job_usage_definition)".

    @Job
    class SendMailJob(
        private val receiverAddress: String
    ) {
        fun execute() {
            // do actual work
        }
    }

    Continuation.job( // 
        Instant.now().plus(Duration.ofMinutes(5)), // 
        SendMailJob("receiver@example.com") // 
    )

-   construct the time the job should be scheduled for (based on domain
    logic)

-   construct an instance of the desired job definition

-   use the returned intent to tell FluxFlow to schedule the job for
    execution

If the scheduled time has already passed, the job will be up for
immediate execution. There are no guarantees on how this immediate
execution will be performed. Depending on the scheduler, the job might
be executed synchronously within the current thread and context or
asynchronously with a small technical delay.

### Using a step action continuation

If an instance of `JobContinuation` is returned by a step action, it is
automatically picked up and schedule by FluxFlow. There is no need to
schedule it explicitly.

**Scheduling a mail notification once a step action is executed**

    @Step
    class CheckVacationRequest {
        @Action
        fun permit(): Continuation<*> {
           return Continuation.job(
                Instant.now().plus(Duration.ofMinutes(5)),
                SendMailJob("receiver@example.com")
            )
        }
    }

### Using a job’s payload function

Another common way is to return a `JobContinuation` directly from
another job’s payload function. This way it is possible to mimic
recurring scheduling behavior.

Assuming we want to notify a user every ten minutes, we could return a
new JobContinuation every time it is run.

    @Job
    class SendReminderJob(
        private val mailService: MailService,
        val receiver: String
    ) {
        fun sendReminder(): Continuation<*> {
            mailService.sendReminder(receiver)
            return Continuation.job( // 
                Instant.now().plus(Duration.ofMinutes(10)), // 
                SendReminderJob(
                    mailService,
                    receiver
                )
            )
        }
    }

-   Return a continuation that schedules a new job, which

-   should be executed in ten minutes.

When rescheduling a job, it is important to include an exit condition to
avoid infinite loops or unexpected executions.

### Using a service method

This way of scheduling a job would be the fallback solution. In most
cases on of the other ways should be preferred.

Both the `WorkflowService` and `JobService` provide a function that can
receive a `JobContinuation`. While the `WorkflowService.start(...)`
function would be used if a new workflow should be started for the job,
the `JobService.schedule` function can be used if a job should be
scheduled for a preexisting workflow.

## Cancellation

FluxFlow supports job cancellation based on a "cancellation key". Such
cancellation key is a unique string identifier assigned to a workflow’s
job. The idea behind this, is that there must never be more than one
scheduled job for each workflow and cancellation key.

Note that jobs can only be canceled as long as they are within the
`Scheduled` status. As soon as execution has begun, they can no longer
be canceled.

Cancellation keys are workflow-scoped. Canceling any given cancellation
key will not affect jobs having the same cancellation key, as long as
they belong to other workflows.

### Implicit cancellation by scheduling a new job

One way of canceling a job, is by replacing it with a new job. This can
be archived very easily by specifying the same cancellation key twice
during scheduling.

**Canceling a job by replacing it**

    @Job
    class SoundAlarmClockJob {
        fun timeToWakeUp() {
            // Sound alarm as loud as possible
        }
    }

    @Step
    class SleepStep {
        @Action(statusBehavior = ImplicitStatusBehavior.Preserve)
        fun wakeMeUpIn10Minutes(): JobContinuation<SoundAlarmClockJob> {
            return Continuation.job(
                Instant.now().plus(10, ChronoUnit.MINUTES),
                SoundAlarmClockJob(),
                CancellationKey("alarm") // 
            )
        }

        @Action(statusBehavior = ImplicitStatusBehavior.Preserve)
        fun wakeMeUpIn1Hour(): JobContinuation<SoundAlarmClockJob> {
            return Continuation.job(
                Instant.now().plus(1, ChronoUnit.HOURS),
                SoundAlarmClockJob(),
                CancellationKey("alarm") // 
            )
        }
    }

-   Reusing the cancellation key "alarm" for both with cause jobs
    created by `wakeMeUpIn10Minutes` to be replaced jobs created by
    `wakeMeUpIn1Hour` and vice versa.

### Explicit job cancellation

If replacing the job with a new one is not an option, one might return
`Continuation.cancelJobs`. This will cancel all jobs having the same
cancellation key.

**Cancel a job without starting a new one**

    @Job
    class SoundAlarmClockJob {
        fun timeToWakeUp() {
            // Sound alarm as loud as possible
        }
    }

    @Step
    class SleepStep {
        @Action(statusBehavior = ImplicitStatusBehavior.Preserve)
        fun wakeMeUpIn10Minutes(): JobContinuation<SoundAlarmClockJob> {
            return Continuation.job(
                Instant.now().plus(10, ChronoUnit.MINUTES),
                SoundAlarmClockJob(),
                CancellationKey("alarm")
            )
        }

        @Action(statusBehavior = ImplicitStatusBehavior.Preserve)
        fun neverGonnaWakeMeUp(): Continuation<*> {
            return Continuation.cancelJobs( // 
                CancellationKey("alarm")
            )
        }
    }

-   This will cancel all previously scheduled jobs having "alarm" for a
    cancellation key.
