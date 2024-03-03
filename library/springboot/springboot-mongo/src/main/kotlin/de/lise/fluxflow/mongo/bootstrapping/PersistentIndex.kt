package de.lise.fluxflow.mongo.bootstrapping

import org.bson.Document
import org.bson.conversions.Bson

data class PersistentIndex(
    val name: String,
    val key: Bson,
    val unique: Boolean
) {

    constructor(doc: Document) : this(
        doc.get("name", String::class.java),
        doc.get("key", Bson::class.java),
        doc.get("unique", false)
    )

}