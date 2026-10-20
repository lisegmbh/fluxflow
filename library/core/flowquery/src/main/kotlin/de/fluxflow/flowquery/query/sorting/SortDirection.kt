package de.fluxflow.flowquery.query.sorting

enum class SortDirection {
    Ascending,
    Descending;
    
    fun toText(): String {
        return when(this) {
            Ascending -> "ASC"
            Descending -> "DESC"
        }
    }
}