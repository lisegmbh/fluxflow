package de.lise.fluxflow.api.interceptors

/**
 * Represents the execution status within an interceptor chain.
 */
enum class InterceptionTokenStatus(
    /**
     * Indicates if the status is terminal, preventing further interceptors from being invoked.
     */
    val isTerminal: Boolean
) {
    /**
     * Occurs whenever the filter chain has been aborted (by calling [InterceptionToken.abort]).
     */
    Aborted(true),

    /**
     * Indicates that the last interceptor and/or the actual payload function have been executed, 
     * without any interceptor having aborted the chain.    
     */
    Executed(true),

    /**
     * This status will be active as long as the final interceptor has not been called,  
     * and no interceptor opted to abort the chain.  
     */
    Pending(false)
}