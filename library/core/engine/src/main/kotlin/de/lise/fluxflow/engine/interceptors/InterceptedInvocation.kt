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
    
    companion object {
        /**
         * Creates an [InterceptedInvocation] without a single interceptor,
         * effectively calling the payload functions directly. 
         */
        @JvmStatic
        fun <TContext> empty(): InterceptedInvocation<TContext> {
            return InterceptedInvocation(emptyList())
        }
    }
}