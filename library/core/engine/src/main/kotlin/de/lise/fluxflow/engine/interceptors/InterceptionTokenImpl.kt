package de.lise.fluxflow.engine.interceptors

import de.lise.fluxflow.api.interceptors.InterceptionToken
import de.lise.fluxflow.api.interceptors.InterceptionTokenStatus

class InterceptionTokenImpl<out TContext>(
    override val context: TContext,
    private val payload: () -> InterceptionTokenStatus,
) : InterceptionToken<TContext> {

    override var status: InterceptionTokenStatus = InterceptionTokenStatus.Pending
        private set

    override fun abort() {
        if (status.isTerminal) {
            return
        }
        status = InterceptionTokenStatus.Aborted
    }

    override fun next(): InterceptionTokenStatus {
        if (status.isTerminal) {
            return status
        }
        status = payload()
        return status
    }
}