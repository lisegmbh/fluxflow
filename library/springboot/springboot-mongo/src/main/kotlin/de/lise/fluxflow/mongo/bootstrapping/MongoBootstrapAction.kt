package de.lise.fluxflow.mongo.bootstrapping

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Collation
import com.mongodb.client.model.IndexOptions
import de.lise.fluxflow.api.bootstrapping.BootstrapAction
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.getCollectionName
import kotlin.reflect.KProperty1

abstract class MongoBootstrapAction(
    protected val mongoTemplate: MongoTemplate
) : BootstrapAction {
    protected inline fun <reified TDocument : Any> ensureCollection(): MongoCollection<Document> {
        return ensureCollection(
            mongoTemplate.getCollectionName<TDocument>()
        )
    }

    protected fun ensureCollection(name: String): MongoCollection<Document> {
        return mongoTemplate.getCollection(name)
    }

    protected inline fun <reified TDocument : Any> ensureIndex(
        name: String,
        unique: Boolean,
        vararg props: KProperty1<TDocument, *>
    ) {
        ensureIndex(
            name,
            unique,
            null,
            *props
        )
    }

    protected inline fun <reified TDocument : Any> ensureIndex(
        name: String,
        unique: Boolean,
        collation: Collation?,
        vararg props: KProperty1<TDocument, *>
    ) {
        ensureIndex<TDocument>(
            name,
            unique,
            props.associate {
                it.name to 1
            },
            collation
        )
    }
    
    protected inline fun <reified TDocument : Any> ensureIndex(
        indexName: String,
        unique: Boolean,
        keys: Map<String, Any?>,
        collation: Collation? = null
    ) {
        ensureIndex(
            mongoTemplate.getCollectionName<TDocument>(),
            indexName,
            unique,
            keys,
            collation
        )
    }

    protected fun ensureIndex(
        collectionName: String,
        indexName: String,
        unique: Boolean,
        keys: Map<String, Any?>,
        collation: Collation? = null
    ) {
        Logger.debug(
            "Ensuring index \"{}\" for collection \"{}\" (unique: {}): {}",
            indexName,
            collectionName,
            unique,
            keys.keys.joinToString(", ")
        )
        val collection = ensureCollection(collectionName)
        val indexes = collection.listIndexes().map {
            PersistentIndex(it)
        }
        val keyDoc = Document(keys)

        indexes.firstOrNull { it.name == indexName }?.let {
            if (matchesDesiredIndex(it, keyDoc, collation)) {
                Logger.debug(
                    "Index \"{}\" already configured correctly on collection \"{}\".",
                    indexName,
                    collectionName
                )
                return
            }

            Logger.info(
                "Dropping preexisting incompatible index \"{}\" for collection \"{}\".",
                indexName,
                collectionName
            )
            collection.dropIndex(indexName)
        }
        Logger.info(
            "Created index \"{}\" (unique: {}) for collection \"{}\", with keys: {}",
            indexName,
            unique,
            collectionName,
            keys.keys.joinToString(", ")
        )
        collection.createIndex(
            keyDoc,
            collation.let {
                IndexOptions().name(indexName).unique(unique).collation(collation)
            } ?: IndexOptions().name(indexName).unique(unique)
        )


    }

    private fun matchesDesiredIndex(
        existingIndex: PersistentIndex,
        desiredIndex: Document,
        desiredCollation: Collation?
    ): Boolean {
        if (existingIndex.key.toBsonDocument() != desiredIndex.toBsonDocument()) {
            return false
        }
        if (desiredCollation == null) {
            return true
        }
        
        val existingCollation = existingIndex.collation ?: return false
        val desiredCollationDocument = desiredCollation.asDocument()
        val hasChangedProperties = desiredCollationDocument.entries.any { 
            existingCollation.get(it.key) != it.value
        }
        
        return !hasChangedProperties
    }

    private companion object {
        val Logger = LoggerFactory.getLogger(MongoBootstrapAction::class.java)!!
    }
}