package de.lise.fluxflow.mongo

/** Identifies a document location where the raw audit could not complete its bounded inspection. */
internal class MongoTypeAuditInspectionException(
    val path: String,
    message: String,
) : IllegalArgumentException(message)
