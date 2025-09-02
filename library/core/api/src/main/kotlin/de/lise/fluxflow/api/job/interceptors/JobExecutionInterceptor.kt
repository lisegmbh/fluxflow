package de.lise.fluxflow.api.job.interceptors

import de.lise.fluxflow.api.interceptors.FlowInterceptor

/**
 * A [JobExecutionInterceptor] can be used to intercept immediate job executions.
 *
 * **Note:** Cancelling an execution prevents FluxFlow from updating any information
 * related to the affected job, including (but not limited to) its status.
 *
 * Interceptors that cancel a job execution are responsible for performing the necessary actions, such as:
 * - updating the job status (according to the cancellation reason)
 * - emitting required events
 * - scheduling required continuations
 */
interface JobExecutionInterceptor : FlowInterceptor<JobExecutionContext>