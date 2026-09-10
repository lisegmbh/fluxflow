package de.lise.fluxflow.mongo

import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import org.bson.Document
import org.springframework.data.mongodb.core.convert.MongoTypeMapper

/** Resolves the discriminator key without reflecting into Spring Data internals. */
internal object MongoTypeKeyResolver {
    fun resolve(typeMapper: MongoTypeMapper): String {
        val probe = Document()
        typeMapper.writeType(WorkflowDocument::class.java, probe)
        check(probe.size == 1) {
            "FluxFlow Mongo persistence requires exactly one configured type metadata key"
        }
        return probe.keys.single()
    }
}
