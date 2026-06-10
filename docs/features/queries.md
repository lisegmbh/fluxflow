# Query System

FluxFlow provides a powerful type-safe query API through the `de.fluxflow.flowquery` package.
This system allows you to build complex queries with filtering conditions,
sorting rules,
and pagination settings.

!!! warning "Deprecation Notice"
    The old query API (`JobQuery`, `ContinuationRecordQuery`, etc.) is deprecated
    and will be removed in a future release.
    Please migrate to the new FlowQuery API described below.

## Overview

The FlowQuery API combines three main components:

- **Type-safe filtering expressions** - Build compile-time checked filter conditions
  using [expression operators](#expression-operators)
- **Sorting rules** - Define result ordering using multiple criteria
- **Pagination settings** - Control result set size and paging

## Building Queries

### Filtering

Filtering allows you to limit result sets based on type-safe conditions.
Each queryable domain object exposes its filterable properties through a corresponding `*Queryable` interface.

See the [Expression Operators](#expression-operators) section for a complete list of available filtering operations.

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

The FlowQuery API provides various filtering operations,
grouped into the following categories:

### Conditional Filtering

When filters depend on optional input values,
you can use `ifPresent` to conditionally append a predicate.

A value is considered absent when it is:
- `null`
- a blank `CharSequence`
- an empty `Collection`
- an empty `Map`
- an empty `Array`

```kotlin
data class WorkflowSearchRequest(
    val city: String?,
    val statuses: List<String>?,
)

val query = FlowQuery.of<WorkflowQueryable>()
    .ifPresent(request.city) { city ->
        get(WorkflowQueryable::model)
            .get(PizzaOrder::city)
            .isEqual(city)
    }
    .ifPresent(request.statuses) { statuses ->
        get(WorkflowQueryable::status)
            .isAnyOf(statuses)
    }
```

This keeps query building fluent and avoids manual `if` branches around each optional filter.

| Category                            | Operations                                                   |
|-------------------------------------|--------------------------------------------------------------|
| [Logical](#logical-operators)       | `allTrue`, `anyTrue`, `and`, `or`, `not`                     |
| [Comparison](#comparison-operators) | `isEqual`, `isNotEqual`, `isGreaterThan`, `isLessThan`, etc. |
| [String](#string-operators)         | `startsWith`, `endsWith`, `contains`                         |
| [Collection](#collection-operators) | `contains`, `containsElementThat`                            |

### Sorting

Sorting defines the order of results.
You can combine multiple sorting criteria that are applied in sequence.

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

When sorting on a property of a workflow model subtype, use `asType` to narrow the model first.
Workflows whose model does not match the cast type (or a known subtype) keep their place in the
result set and receive a `null` sort key. Null placement follows the backend defaults
(nulls-first on ascending, nulls-last on descending). Filter semantics for `asType` are unchanged.

### Pagination

To retrieve paginated results,
use the `paged` method on your query:

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
    See the [Sorting](#sorting) section for details.

## Advanced Usage

### Combining Operations

FlowQuery operations can be chained to create complex queries.
All operations are immutable - each one returns a new FlowQuery instance.

The following example combines [filtering](#filtering),
[sorting](#sorting),
and [pagination](#pagination):

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

## Expression Operators

FluxFlow provides a comprehensive set of expression operators for building type-safe queries. These operators are
available
either directly on expressions or through extension functions.

### Operator Overview

| Category       | Operator               | Description                           | Source    | Import Required                                        |
|----------------|------------------------|---------------------------------------|-----------|--------------------------------------------------------|
| **Logical**    | [`allTrue`](#alltrue)              | Combines multiple predicates with AND | Direct    | No                                                     |
|                | [`anyTrue`](#anytrue)              | Combines multiple predicates with OR  | Direct    | No                                                     |
|                | [`and`](#and-extension)                  | Combines two predicates with AND      | Extension | `ExpressionExtensions.Logical.and`                     |
|                | [`or`](#or-extension)                   | Combines two predicates with OR       | Extension | `ExpressionExtensions.Logical.or`                      |
|                | [`not`](#not-extension)                  | Negates a predicate                   | Extension | `ExpressionExtensions.Logical.not`                     |
| **Comparison** | [`isEqual`](#isequal)              | Tests equality                        | Direct    | No                                                     |
|                | [`isNotEqual`](#isnotequal)           | Tests inequality                      | Direct    | No                                                     |
|                | [`isLessThan`](#islessthan)           | Less than comparison                  | Direct    | No                                                     |
|                | [`isLessThanOrEqual`](#islessthanorequal)    | Less than or equal comparison         | Direct    | No                                                     |
|                | [`isGreaterThan`](#isgreaterthan)        | Greater than comparison               | Direct    | No                                                     |
|                | [`isGreaterThanOrEqual`](#isgreaterthanorequal) | Greater than or equal comparison      | Direct    | No                                                     |
|                | [`isAnyOf`](#isanyof)              | Tests if value matches any in a set   | Direct    | No                                                     |
| **String**     | [`startsWith`](#startswith)           | Tests string prefix                   | Extension | `ExpressionExtensions.Strings.startsWith`              |
|                | [`endsWith`](#endswith)             | Tests string suffix                   | Extension | `ExpressionExtensions.Strings.endsWith`                |
|                | [`contains`](#contains-string)             | Tests substring presence              | Extension | `ExpressionExtensions.Strings.contains`                |
| **Collection** | [`contains`](#contains-collection)             | Tests element presence                | Extension | `ExpressionExtensions.Collections.contains`            |
|                | [`containsElementThat`](#containselementthat)  | Tests for matching element            | Extension | `ExpressionExtensions.Collections.containsElementThat` |
| **Type**       | [`isType`](#istype)               | Tests type of value                   | Extension | `ExpressionExtensions.Types.isType`                    |
|                | [`asType`](#astype)               | Casts value to type                   | Extension | `ExpressionExtensions.Types.asType`                    |
| **Property**   | [`get`](#get-direct)                  | Accesses property value               | Direct    | No                                                     |
| **Map**        | [`get`](#get-extension)                  | Accesses map value by key             | Extension | `ExpressionExtensions.Maps.get`                        |

### Logical Operators

#### allTrue

| **Overview**    | Creates a predicate that combines multiple boolean expressions with AND |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | None required (direct method)                                          |
| **Availability**| Direct method                                                          |

The `allTrue` operator creates a predicate that combines multiple boolean expressions with logical AND. All provided predicates must evaluate to true for the result to be true.

**Usage:**

```kotlin
query.where { root ->
    root.allTrue(
        { get(User::active).isEqual(true) },
        { get(User::age).isGreaterThan(18) }
    )
}
```

#### and (Extension)

| **Overview**    | Combines two predicates with AND using infix notation                  |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | `import de.fluxflow.flowquery.expression.ExpressionExtensions.Logical.and` |
| **Availability**| Extension function                                                    |

The `and` operator combines two predicates with logical AND using infix notation, improving readability for simple conjunctions.

**Usage:**

```kotlin
query.where { root ->
    get(User::active).isEqual(true) and
            get(User::age).isGreaterThan(18)
}
```

#### anyTrue

| **Overview**    | Creates a predicate that combines multiple boolean expressions with OR |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | None required (direct method)                                          |
| **Availability**| Direct method                                                          |

The `anyTrue` operator creates a predicate that combines multiple boolean expressions with logical OR. The result is true if any predicate evaluates to true.

**Usage:**

```kotlin
query.where { root ->
    root.anyTrue(
        { get(User::role).isEqual("admin") },
        { get(User::role).isEqual("manager") }
    )
}
```

#### or (Extension)

| **Overview**    | Combines two predicates with OR using infix notation                   |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | `import de.fluxflow.flowquery.expression.ExpressionExtensions.Logical.or` |
| **Availability**| Extension function                                                    |

The `or` operator combines two predicates with logical OR using infix notation, improving readability for simple disjunctions.

**Usage:**

```kotlin
query.where { root ->
    get(User::role).isEqual("admin") or
            get(User::role).isEqual("manager")
}
```

#### not (Extension)

| **Overview**    | Negates a predicate expression                                         |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | `import de.fluxflow.flowquery.expression.ExpressionExtensions.Logical.not` |
| **Availability**| Extension function                                                    |

The `not` operator negates a predicate expression, inverting its boolean result.

**Usage:**

```kotlin
query.where { root ->
    get(User::active).isEqual(false).not()
}
```

### Comparison Operators

#### isEqual

| **Overview**    | Tests if two values are equal                                          |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | None required (direct method)                                          |
| **Availability**| Direct method                                                          |

The `isEqual` operator performs an equality comparison between values. It supports comparing against constant values or other expressions, using the standard Kotlin equality check internally.

**Usage with constant:**

```kotlin
query.where { root ->
    get(User::name).isEqual("John")
}
```

**Usage with expression:**

```kotlin
query.where { root ->
    // Compare with another property
    get(User::age).isEqual(
        get(User::minimumAge)
    )
}
```


#### isNotEqual

| **Overview**    | Tests if two values are not equal                                      |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | None required (direct method)                                          |
| **Availability**| Direct method                                                          |

The `isNotEqual` operator compares values for inequality. It can compare against constant values or other expressions, using the standard Kotlin equality check internally.

**Usage with constant:**

```kotlin
query.where { root ->
    get(User::status).isNotEqual("inactive")
}
```

**Usage with expression:**

```kotlin
query.where { root ->
    // Compare with another property
    get(User::currentRole).isNotEqual(
        get(User::previousRole)
    )
}
```


#### isLessThan

| **Overview**    | Tests if one value is less than another                                |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | None required (direct method)                                          |
| **Availability**| Direct method                                                          |

The `isLessThan` operator tests if one value is less than another.

**Usage:**

```kotlin
query.where { root ->
    get(User::age).isLessThan(18)
}
```

#### isLessThanOrEqual

| **Overview**    | Tests if one value is less than or equal to another                    |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | None required (direct method)                                          |
| **Availability**| Direct method                                                          |

The `isLessThanOrEqual` operator tests if one value is less than or equal to another.

**Usage:**

```kotlin
query.where { root ->
    get(Order::amount).isLessThanOrEqual(1000.0)
}
```

#### isGreaterThan

| **Overview**    | Tests if one value is greater than another                             |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | None required (direct method)                                          |
| **Availability**| Direct method                                                          |

The `isGreaterThan` operator tests if one value is greater than another.

**Usage:**

```kotlin
query.where { root ->
    get(User::age).isGreaterThan(18)
}
```

#### isGreaterThanOrEqual

| **Overview**    | Tests if one value is greater than or equal to another                 |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | None required (direct method)                                          |
| **Availability**| Direct method                                                          |

The `isGreaterThanOrEqual` operator tests if one value is greater than or equal to another.

**Usage:**

```kotlin
query.where { root ->
    get(Order::amount).isGreaterThanOrEqual(100.0)
}
```

#### isAnyOf

| **Overview**    | Tests if a value matches any value in a given set                      |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | None required (direct method)                                          |
| **Availability**| Direct method                                                          |

The `isAnyOf` operator tests if a value matches any value in a given set or collection.

**Usage:**

```kotlin
query.where { root ->
    get(User::status).isAnyOf("ACTIVE", "PENDING")
    // Or with a collection:
    get(User::status).isAnyOf(listOf("ACTIVE", "PENDING"))
}
```

### String Operators

#### startsWith

| **Overview**    | Tests if a string starts with a given prefix                           |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | `import de.fluxflow.flowquery.expression.ExpressionExtensions.Strings.startsWith` |
| **Availability**| Extension function                                                    |

The `startsWith` operator tests if a string starts with a given prefix. You can also specify case sensitivity.

**Usage:**

```kotlin
query.where { root ->
    get(User::email).startsWith("admin@")
    // With case sensitivity:
    get(User::email).startsWith("ADMIN@", ignoreCasing = false)
}
```

#### endsWith

| **Overview**    | Tests if a string ends with a given suffix                            |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | `import de.fluxflow.flowquery.expression.ExpressionExtensions.Strings.endsWith` |
| **Availability**| Extension function                                                    |

The `endsWith` operator tests if a string ends with a given suffix.

**Usage:**

```kotlin
query.where { root ->
    get(User::email).endsWith("@company.com")
}
```

#### contains (String)

| **Overview**    | Tests if a string contains a given substring                          |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | `import de.fluxflow.flowquery.expression.ExpressionExtensions.Strings.contains` |
| **Availability**| Extension function                                                    |

The `contains` operator tests if a string contains a given substring. You can also specify case sensitivity.

**Usage:**

```kotlin
query.where { root ->
    get(User::name).contains("John")
    // Case-sensitive search:
    get(User::name).contains("JOHN", ignoreCasing = false)
}
```

### Collection Operators

#### contains (Collection)

| **Overview**    | Tests if a collection contains a specific element                     |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | `import de.fluxflow.flowquery.expression.ExpressionExtensions.Collections.contains` |
| **Availability**| Extension function                                                    |

The `contains` operator tests if a collection contains a specific element, or the value of another expression.

**Usage:**

```kotlin
query.where { root ->
    get(User::roles).contains("ADMIN")
    // Or with an expression:
    get(User::roles).contains(other.get(Role::name))
}
```

#### containsElementThat

| **Overview**    | Tests if a collection contains any element matching a predicate        |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | `import de.fluxflow.flowquery.expression.ExpressionExtensions.Collections.containsElementThat` |
| **Availability**| Extension function                                                    |

The `containsElementThat` operator tests if a collection contains any element matching a predicate.

**Usage:**

```kotlin
query.where { root ->
    get(User::orders).containsElementThat {
        get(Order::amount).isGreaterThan(1000.0)
    }
}
```

### Type Operators

#### isType

| **Overview**    | Tests if a value is of a specific type or any of its subtypes         |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | `import de.fluxflow.flowquery.expression.ExpressionExtensions.Types.isType` |
| **Availability**| Extension function                                                    |

For MongoDB repositories, the type check is implemented using the `_class` field. The check includes the specified type and all its non-abstract subclasses discovered by scanning the application's base packages.

**Usage:**

```kotlin
query.where { root ->
    get(User::address).isType(ShippingAddress::class)
}
```

!!! note "MongoDB Implementation"
    - Translates to a MongoDB `$in` query on the `_class` field
    - Automatically includes all non-abstract subclasses
    - Requires classes to be in application's base packages

!!! warning "Common Pitfalls"
    - Classes outside base packages won't be found
    - Relies on MongoDB's `_class` field being correctly set
    - Class name or package changes affect existing queries

**Example with inheritance:**

```kotlin
// Define a class hierarchy
abstract class Address
class ShippingAddress : Address()
class BillingAddress : Address()

// Query will match both ShippingAddress and any non-abstract subclass of ShippingAddress
query.where { root ->
    get(User::address).isType(ShippingAddress::class)
}
```

#### asType

| **Overview**    | Casts a value to a more specific type                                 |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | `import de.fluxflow.flowquery.expression.ExpressionExtensions.Types.asType` |
| **Availability**| Extension function                                                    |

The `asType` operator casts a value to a more specific type for further property access or filtering.

**Usage:**

```kotlin
query.where { root ->
    get(User::address).asType(ShippingAddress::class)
        .get(ShippingAddress::country).isEqual("DE")
}
```

### Property Access

#### get (Direct)

| **Overview**    | Accesses a property value of an object                                |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | None required (direct method)                                         |
| **Availability**| Direct method                                                         |

The operator provides type-safe access to properties of the current expression's value type. It works with any Kotlin property that can be represented by `KProperty1`. The returned expression preserves type information for further operations.

**Usage with single property:**

```kotlin
query.where { root ->
    get(User::name).isEqual("John")
}
```

**Usage with chained properties:**

```kotlin
query.where { root ->
    get(User::address).get(Address::city).isEqual("Berlin")
}
```


### Map Operations

#### get (Extension)

| **Overview**    | Accesses a value in a map by key                                      |
|-----------------|-----------------------------------------------------------------------|
| **Import**      | `import de.fluxflow.flowquery.expression.ExpressionExtensions.Maps.get` |
| **Availability**| Extension function                                                    |

The `get` operator for maps accesses a value in a map by key, either as a constant or from another expression.

**Usage:**

```kotlin
query.where { root ->
    get(User::attributes).get(const("role")).isEqual("admin")
    // Or with a dynamic key:
    get(User::attributes).get(other.get(Setting::key)).isEqual("value")
}
```
