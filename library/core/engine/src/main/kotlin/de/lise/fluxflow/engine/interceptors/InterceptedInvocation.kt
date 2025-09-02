package de.lise.fluxflow.engine.interceptors

import de.lise.fluxflow.api.interceptors.FlowInterceptor
import de.lise.fluxflow.api.interceptors.InterceptionTokenStatus

class InterceptedInvocation<in TContext>(
    private val interceptors: List<FlowInterceptor<TContext>>,
) {
    fun invoke(
        context: TContext,
        executable: () -> Unit
    ): InterceptionTokenStatus {
        val finalToken = InterceptionTokenImpl(
            context,
        ) {
            executable()
            InterceptionTokenStatus.Executed
        }
        
        var rootToken = finalToken
        for(interceptor in interceptors.reversed()) {
            val nextToken = rootToken
            rootToken = InterceptionTokenImpl(
                context
            ) {
                interceptor.intercept(nextToken)
                if(!nextToken.status.isTerminal) {
                    nextToken.next()
                }
                nextToken.status
            }
        }
        
        rootToken.next()
        
        return rootToken.status
    }
}