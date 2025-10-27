package de.fluxflow.flowquery.mapper.query

import de.fluxflow.flowquery.query.FlowQuery

interface QueryMapper<TFromRoot, TToRoot> {
    fun <TNewResult> map(query: FlowQuery<TFromRoot, *>): FlowQuery<TToRoot, TNewResult>
}

