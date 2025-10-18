package de.fluxflow.flowquery.repository

interface FlowQueryRepository<TRoot> : NonProjectingRepository<TRoot>, ProjectingRepository<TRoot>

