# Queries
The possibility to search for FluxFlow objects is provided by the
`de.lise.fluxflow.api.query` package. A query can include an optional
filter, one or more sorting rules and an optional pagination request.

## Query
The query object combines

1.  filters
2.  sorting rules and
3.  pagination settings

that should be applied when fetching FluxFlow objects.

## Filtering

Filtering is used to limit the result set returned by a query, based on
certain criteria. There is a specific filter class for each filterable
domain object, allowing for targeted queries.

One of those object-specific filter classes is the `WorkflowFilter`. It
exposes the filterable properties of a workflow and provides some
builder methods.

In order to get started, one would create an instance of this class by
directly invoking the constructor

**Direct workflow filter creation**

    data class PizzaOrder(val city: String)
    
    val myWorkflowFilter = WorkflowFilter<PizzaOrder>(
        id = null,
        model = Filter.property(
            PizzaOrder::class,
            Filter.eq("Cologne")
        )
    )

or using the builder methods.

**Workflow filter creation using the builder methods**

    data class PizzaOrder(val city: String)

    val filter = WorkflowFilter.empty<PizzaOrder>()
            .withModelFilter(
                Filter.property(
                    PizzaOrder::city,
                    Filter.eq("Cologne")
                )
            )

The `WorkflowFilter` 's job is to guide the developer during filter
specification, while the generic `Filter<T>` implementations allow for
use-case specific filter definitions.

To see a full listing of supported operations, refer to the static
filter builder methods within the `Filter<T>` interface.

## Sorting

Sorting defines the order that should be applied to the result set. It
is possible to combine multiple sorting criteria. When two or more
objects are equal based on a certain sorting criteria, the next sorting
rule will be applied for those objects. This process is repeated until a
definit sorting order is archived or there is no more criteria left
over.

The sorting definition is constructed similar to filter definitions.

**Workflow sorting based on a model property**

    data class PizzaOrder(val city: String, val invoicedAmount: Double)

    val sortByCityAsc = WorkflowSort.model(
            Sort.property(
                PizzaOrder::city,
                Sort.asc()
            )
        )

Within a query, you can then combine those filters.

**Sort workflows by city, invoiced amount and finally by their
identifiers**

    data class PizzaOrder(val city: String, val invoicedAmount: Double)

    WorkflowQuery.empty<WorkflowFilter<PizzaOrder>, WorkflowSort<PizzaOrder>>()
            .addSort(
                WorkflowSort.model(
                    Sort.property(
                        PizzaOrder::city,
                        Sort.asc()
                    )
                )
            ).addSort(
                WorkflowSort.model(
                    Sort.property(
                        PizzaOrder::invoicedAmount,
                        Sort.desc()
                    )
                )
            ).addSort(WorkflowSort.identifier(Direction.Ascending))

## Pagination

In order to obtain a paginated result set, the
`Query.withPage(pageIndex: Int, pageSize: Int)` or
`Query.withPage(pageRequest: PaginationRequest)` method can be used.
Both require the page index (starting at zero) of the page to be fetched
and the page size specifying the maximum number of elements that might
be contained within a page.

Note that it is possible for a page to contain fewer elements than the
requested page size. Especially if there are not enough elements to fill
that page.

In order to archive reproducible results, a sort order should be
defined.

**Pagination for workflow queries**

    data class PizzaOrder(val city: String)

    val query: WorkflowQuery<PizzaOrder> = Query.empty<WorkflowFilter<PizzaOrder>, WorkflowSort<PizzaOrder>>()
            .withPage(0, 50)
