package de.lise.fluxflow.mongo.bootstrapping

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.IndexOptions
import de.lise.fluxflow.api.bootstrapping.BootstrapAction
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.getCollectionName
import kotlin.reflect.KProperty1

abstract class MongoBootstrapAction(
    protected val mongoTemplate: MongoTemplate
): BootstrapAction {
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
        vararg props: KProperty1<TDocument,*>
    ) {
        ensureIndex<TDocument>(
            name,
            unique,
            props.associate { 
                it.name to 1
            }
        )
    }
    
    protected inline fun <reified TDocument : Any> ensureIndex(
        indexName: String,
        unique: Boolean,
        keys: Map<String, Any?>
    ) {
        ensureIndex(
            mongoTemplate.getCollectionName<TDocument>(),
            indexName,
            unique,
            keys
        )
    }
    
    protected fun ensureIndex(
        collectionName: String,
        indexName: String,
        unique: Boolean,
        keys: Map<String, Any?>
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
            if(it.key.toBsonDocument() == keyDoc.toBsonDocument()) {
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
            IndexOptions().name(indexName).unique(unique)
        )
    }

    private companion object {
        val Logger = LoggerFactory.getLogger(MongoBootstrapAction::class.java)!!
    }
}