package de.lise.fluxflow.api.continuation.history.query.filter

import de.lise.fluxflow.api.continuation.history.ContinuationRecordIdentifier
import de.lise.fluxflow.query.filter.Filter

data class ContinuationRecordIdentifierFilter(
    val value: Filter<String>
) {
    companion object {
        fun eq(continuationRecordIdentifier: ContinuationRecordIdentifier): ContinuationRecordIdentifierFilter {
            return ContinuationRecordIdentifierFilter(
                Filter.eq(continuationRecordIdentifier.value)
            )
        }

        fun anyOf(continuationRecordIdentifiers: Collection<ContinuationRecordIdentifier>): ContinuationRecordIdentifierFilter {
            return ContinuationRecordIdentifierFilter(
                Filter.anyOf(
                    continuationRecordIdentifiers.map { it.value }
                )
            )
        }
    }
}