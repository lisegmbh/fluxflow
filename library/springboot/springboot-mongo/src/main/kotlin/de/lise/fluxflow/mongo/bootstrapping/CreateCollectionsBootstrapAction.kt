package de.lise.fluxflow.mongo.bootstrapping

import de.lise.fluxflow.mongo.bootstrapping.collation.CollationConfigurer
import de.lise.fluxflow.mongo.continuation.history.ContinuationRecordDocument
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import org.springframework.data.mongodb.core.CollectionOptions
import org.springframework.data.mongodb.core.MongoTemplate

/**
 * This bootstrap action is used
 * to enforce that all FluxFlow collections have initially been created using the desired collation defaults.
 *
 * This bootstrap action should be the first action to interact with the MongoDB.
 */
class CreateCollectionsBootstrapAction(
    mongoTemplate: MongoTemplate,
    private val collationConfigurer: CollationConfigurer
) : MongoBootstrapAction(mongoTemplate) {
    override fun setup() {
        val existingCollectionNames = mongoTemplate.collectionNames

        ensureCollectionCollation<WorkflowDocument>(existingCollectionNames)
        ensureCollectionCollation<StepDocument>(existingCollectionNames)
        ensureCollectionCollation<ContinuationRecordDocument>(existingCollectionNames)
        ensureCollectionCollation<JobDocument>(existingCollectionNames)
    }

    private inline fun <reified TDocument : Any> ensureCollectionCollation(
        allExistingCollections: Set<String>
    ) {
        val javaType = TDocument::class.java
        val expectedCollectionName = mongoTemplate.getCollectionName(javaType)
        if(allExistingCollections.contains(expectedCollectionName)) {
            return
        }

        val collation = collationConfigurer.configureCollation(javaType)
        if(collation == null) {
            mongoTemplate.createCollection(expectedCollectionName)
        } else {
            mongoTemplate.createCollection(
                expectedCollectionName,
                CollectionOptions.just(collation)
            )
        }
    }
}