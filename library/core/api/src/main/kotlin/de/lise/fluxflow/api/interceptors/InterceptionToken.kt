package de.lise.fluxflow.api.interceptors

/**
 * An interception token can be used by [FlowInterceptor]s to influence the control flow.
 */
interface InterceptionToken<out TContext> {
    /**
     * The execution context providing additional context information.
     */
    val context: TContext

    /**
     * The status of this token.
     */
    val status: InterceptionTokenStatus

    /**
     * Aborts the execution.
     * If this function is called, 
     * successive interceptors as well as the original payload will not be executed.
     * 
     * If this token is in a terminal state, a call to this function will have no effect. 
     */
    fun abort()

    /**
     * Immediately continues the interceptor chain invocation.
     * If this token is in a terminal state, a call to this function will have no effect.
     * 
     * @return The new status of this token.
     */
    fun next(): InterceptionTokenStatus
}