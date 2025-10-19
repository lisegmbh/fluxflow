package de.fluxflow.flowquery.mapper.query

import de.fluxflow.flowquery.query.*


interface QueryMapper<TFromRoot, TToRoot> {
    fun <TNewResult> map(query: Query<TFromRoot, *>): Query<TToRoot, TNewResult>
}

