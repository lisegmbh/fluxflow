package de.lise.fluxflow.api.continuation.history.query

import de.lise.fluxflow.api.continuation.history.query.filter.ContinuationRecordFilter
import de.lise.fluxflow.api.continuation.history.query.sort.ContinuationRecordSort
import de.lise.fluxflow.query.Query

typealias ContinuationRecordQuery = Query<ContinuationRecordFilter, ContinuationRecordSort>