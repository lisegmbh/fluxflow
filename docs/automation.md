# Automation

Automation functions enrich workflow steps by embedding predefined
actions that are executed without explicit intervention. They can be
bound to one or more lifecycle events of their parent step and are run
automatically. These actions can encompass a range of tasks, such as
data validation, notification sending, data transformation, or system
integration.

The main difference between an `Action` and `Automation` is, that an
`Action` can and must be invoked explicitly while the `Automation` is
executed automatically.

## Usage

### Basic definition

An automation function is defined similar to an action. Just create a
public and non-abstract function within your step definition and
annotate it with the right annotation (see the following table).

<table>
<caption>Overview of available automation annotations</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Annotation</p></td>
<td style="text-align: left;"><p>Time of execution</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><ol type="1">
<li><p><code>@Automated(Trigger.OnCreated)</code></p></li>
<li><p><code>@OnCreated</code></p></li>
</ol></td>
<td style="text-align: left;"><p>After the owing step has been
created.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><ol type="1">
<li><p><code>@Automated(Trigger.OnCompleted)</code></p></li>
<li><p><code>@OnCompleted</code></p></li>
</ol></td>
<td style="text-align: left;"><p>After the owning step transitioned into
the <code>Status.Completed</code> status.</p></td>
</tr>
</tbody>
</table>

Overview of available automation annotations

The next example will illustrate how to define a basic automation
function that logs a message once the step has been started.

**Basic example on how to define an automation function**

    class CompleteOrderStep(
        val orderId: String
    ) {
        @OnCreated // 
        fun logOrderAboutToComplete() {
            Logger.info("Waiting for completion of order '{}'.", orderId)
        }

        @Action // 
        fun complete() {
            Logger.info("Order '{}' has been completed.", orderId)
        }
    }

-   Use the `OnCreated` annotation to indicate that the
    `logOrderAboutToComplete` should be run automatically. Note that the
    annotation is required, because the function would instead be
    treated as a regular action.

-   The action will only be executed if invoked explicitly. The
    annotation is optional.

Do not use a step’s constructor in order to execute state-modifying
logic, as it might be invoked multiple times.

The next example demonstrates that the constructor is usually not the
right place to implement custom logic, as the workflow engine might need
to invoke it unpredictably and more than once.

**Illustration on when a certain peace of logic is executed**

    class CompleteOrderStep {
        val orderId: String

        constructor(orderId: String) {
            this.orderId = orderId
            // 
            Logger.debug("An instance of CompleteOrderStep for order '{}' created.", orderId)
        }

        @OnCreated // 
        fun logOrderAboutToComplete() {
            Logger.info("Waiting for completion of order '{}'.", orderId)
        }

        @Action // 
        fun complete() {
            Logger.info("Order '{}' has been completed.", orderId)
        }
    }

-   Is invoked whenever the workflow engine needs to construct an
    instance of the step definition. For example, this will happen when
    querying for steps, invoking jobs or executing actions.

-   This "automation function" is guaranteed to be executed once for
    each started step instance.

-   The action is run, when explicitly requested.

### Injection into automation functions

Automation functions also support parameter injection. In order to
obtain dependencies, they can be declared as function parameters.

**Example of an automation function requiring parameters**

    class ConfirmOrderStep {
        @OnCompleted
        fun onConfirmOrder(mailService: MailService) {
            mailService.sendOrderConfirmation(...)
        }
    }

Currently, injection is only supported for values that can be obtained
from the IoC container.

### Working with continuations

An automated function can also return a continuation to trigger
additional steps or schedule jobs. This is done the same way as it is
done for actions.

**Example of an automation function returning a continuation**

    @Job
    class CancelOrderJob(
        val orderId: String
    ) {
        fun cancelOrderDueToInactivity() {
            Logger.warn("Canceling order '{}' due to inactivity.", orderId)
            // do the actual cancellation
        }
    }

    @Step
    class CompleteOrderStep(val orderId: String) {
        @OnCreated
        fun scheduleAutomaticCancellation(): Continuation<*> {
            return Continuation.job(
                Instant.now().plus(Duration.ofMinutes(10)),
                CancelOrderJob(orderId)
            )
        }

        @Action
        fun complete() {
            Logger.info("Order '{}' has been completed.", orderId)
        }
    }

The step status behavior is determined by the returned continuation.
Always set the status behavior explicitly to avoid unexpected status
transitions, when returning a continuation object. If the automation
function doesn’t return a continuation, the step status is always
preserved.

### Running automation functions on multiple triggers

Automation functions can also be executed on multiple triggers. This can
easily be achieved by annotating a function with multiple annotations
representing the relevant trigger events.

    @Step
    class CompleteOrderStep(val orderId: String) {
        @OnCreated
        @OnCompleted
        fun logTransition() {
            Logger.debug("Order '{}' transitioned in to or out of the complete order step.", orderId)
        }
    }

Even though it is supported to mix trigger specific annotations like
`@OnCreated` and the generalized `@Automated(...)` annotation, this
should be avoided to increase readability.
