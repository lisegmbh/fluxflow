package de.lise.fluxflow.query.pagination

data class PaginationRequest(
    val pageIndex: Int,
    val pageSize: Int
)