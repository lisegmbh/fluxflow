package de.fluxflow.flowquery.repository

/**
 * Combines the capabilities of both [NonProjectingRepository] and [ProjectingRepository],
 * providing a unified interface for executing type-safe [FlowQuery] operations.
 *
 * A [FlowQueryRepository] allows:
 * - Retrieving full root entities via the [NonProjectingRepository] API, and
 * - Executing projected or transformed queries via the [ProjectingRepository] API.
 *
 * Implementations typically delegate both sets of operations to a shared query engine
 * or data access layer capable of evaluating expression trees defined by [FlowQuery].
 *
 * @param TRoot the root entity or domain type managed by this repository
 */
interface FlowQueryRepository<TRoot> :
    NonProjectingRepository<TRoot>,
    ProjectingRepository<TRoot>
