package de.fluxflow.flowquery.query

/**
 * An immutable, per-execution restriction interpreted by a supporting persistence adapter.
 * Unsupported constraints must fail instead of falling back to an unrestricted query.
 * Contains no ambient or thread-bound state; callers pass it explicitly with the query.
 */
interface QueryConstraint
