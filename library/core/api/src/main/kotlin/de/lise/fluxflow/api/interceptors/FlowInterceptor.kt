package de.lise.fluxflow.api.interceptors

/**
 * A [FlowInterceptor] can be used to manipulate the control flow before invoking a certain payload.
 * 
 * The [intercept] function will be invoked before the actual payload. 
 * 
 * Implementations might do something right before 
 * and/or after the actual payload execution by calling [InterceptionToken.next] -
 * e.g.
 * ```kotlin
 * FlowInterceptor<Any> { token ->
 *     // Setup code
 *     customSetupLogic()
 * 
 *     // Executing the next interceptor 
 *     // and eventually the actual payload
 *     val result = token.next()
 * 
 *     when (result) {
 *         // The payload has been executed
 *         InterceptionTokenStatus.Executed -> customTeardownLogic()
 *         // The payload has not been executed
 *         else -> {}
 *     }
 * }
 * ```
 * Doing so will continue the chain recursively 
 * and synchronously within the current interceptor's scope.
 * 
 * If an interceptor opts to abort the execution,
 * it can do so by invoking the [InterceptionToken.abort] function.
 * This call returns immediately and prevents any further interceptor from being executed.
 * 
 * Whenever an interceptor returns without having called [InterceptionToken.next] or [InterceptionToken.abort],
 * the interceptor chain will be continued iterative.
 */
fun interface FlowInterceptor<in TContext> {
    fun intercept(token: InterceptionToken<TContext>)
}