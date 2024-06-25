# Unreleased

## MongoDB type mappings
Previous versions used to store Java types using a simple key-value mapping,
where the key contained the data key, and the value held the associated Java type. 
This has proven to be problematic,
as migrations needed to do complicated queries, just to identify and update every occurrence of a given type.
To support developers needing to do type migrations (e.g., when renaming or moving a type),
the used data structure has been optimized to allow for more efficient queries.

The migration to the new type storage structure unfortunately requires loading all affected documents from the database
and transforming their data structures using application logic.
As this might lead to unexpected failures when reactivating old/missing/modified types, 
**a database backup is strongly recommended**.

Depending on the number of documents to be migrated,
**this process might require a substantial amount of time and computing resources**.

### Possible errors when migrating old/missing/modified/types
When the application is started, it will automatically try to migrate all old database entries.
If there is a failure when migrating a document,
the application startup is going to be terminated with an exception reading:

> Type record migration failed for one or more documents (see https://docs.fluxflow.cloud/see/1): [...]

The exception message will furthermore list all problematic documents.

#### Solution
The affected documents (as listed by the exception message) need manual attention/migration.

To fix the issue, a developer can use one of the following approaches:
1. remove the problematic document
2. migrate their type records manually
3. fix the issue that prevents the type's reactivation (e.g., re-adding missing types)

Another possibility is to explicitly allow FluxFlow to ignore documents that can't be migrated.
This will skip all affected documents, allowing the application startup to complete.
In effect, the type activation error is hereby delayed until the affected resource is actually used during execution.

To do so, set `fluxflow.mongo.migrations.typeRecords` to `warn`.

```yaml
fluxflow.mongo.migrations.typeRecords: warn
```