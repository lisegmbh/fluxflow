# Query System

FluxFlow provides a powerful type-safe query API through the `de.fluxflow.flowquery` package. This system allows you to build
complex queries with filtering conditions, sorting rules, and pagination settings.

!!! warning "Deprecation Notice"
    The old query API (`JobQuery`, `ContinuationRecordQuery`, etc.) is deprecated and will be removed in a future release.
    Please migrate to the new FlowQuery API described below.

## Overview

The FlowQuery API combines three main components:

- **Type-safe filtering expressions** - Build compile-time checked filter conditions
- **Sorting rules** - Define result ordering using multiple criteria
- **Pagination settings** - Control result set size and paging

## Building Queries

### Filtering

Filtering allows you to limit result sets based on type-safe conditions. Each queryable domain object exposes its 
filterable properties through a corresponding `*Queryable` interface.

**Example using WorkflowQueryable:**

```kotlin
data class PizzaOrder(val city: String)

// Create a query filtering workflows by city
val query = FlowQuery.of<WorkflowQueryable>()
    .where { 
        get(WorkflowQueryable::model)
            .get(PizzaOrder::city)
            .isEqual("Cologne") 
    }
```

The FlowQuery API provides these filtering operations:

| Category | Operations |
|----------|------------|
| Comparison | `isEqual`, `isNotEqual`, `isGreaterThan`, `isLessThan` |
| String | `startsWith`, `endsWith`, `contains` |
| Collection | `contains`, `containsElementThat` |
| Logical | `and`, `or`, `not` |

### Sorting

Sorting defines the order of results. You can combine multiple sorting criteria that are applied in sequence.

```kotlin
data class PizzaOrder(val city: String, val invoicedAmount: Double)

// Sort workflows by city (ascending) then by invoiced amount (descending)
val query = FlowQuery.of<WorkflowQueryable>()
    .sort { 
        Sorting.asc(get(WorkflowQueryable::model).get(PizzaOrder::city))
    }
    .sort { 
        Sorting.desc(get(WorkflowQueryable::model).get(PizzaOrder::invoicedAmount))
    }
```

### Pagination

To retrieve paginated results, use the `paged` method on your query:

```kotlin
data class PizzaOrder(val city: String)

// Get the first page with 50 items per page
val query = FlowQuery.of<WorkflowQueryable>()
    .paged(pageIndex = 0, pageSize = 50)

// Or use a PaginationRequest object
val query = FlowQuery.of<WorkflowQueryable>()
    .paged(PaginationRequest(pageIndex = 0, pageSize = 50))
```

!!! tip "Best Practice"
    Always define a sort order when using pagination to ensure reproducible results.

## Advanced Usage

### Combining Operations

FlowQuery operations can be chained to create complex queries. All operations are immutable - each one returns a new 
FlowQuery instance.

```kotlin
data class PizzaOrder(val city: String, val invoicedAmount: Double)

val query = FlowQuery.of<WorkflowQueryable>()
    .where { 
        get(WorkflowQueryable::model)
            .get(PizzaOrder::city)
            .isEqual("Cologne") 
    }
    .sort { 
        Sorting.asc(get(WorkflowQueryable::model).get(PizzaOrder::invoicedAmount))
    }
    .paged(pageIndex = 0, pageSize = 20)
```
