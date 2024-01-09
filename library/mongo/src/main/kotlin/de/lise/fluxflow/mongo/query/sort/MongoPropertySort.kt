package de.lise.fluxflow.mongo.query.sort

import de.lise.fluxflow.mongo.query.getMongoFieldName
import org.springframework.data.domain.Sort
import kotlin.reflect.KProperty1

data class MongoPropertySort<TObject, TProperty>(
    private val property: KProperty1<TObject, TProperty>,
    private val sortForProperty: MongoSort<TProperty>
): MongoSort<TObject> {
    override fun apply(path: String): Sort {
        return sortForProperty.apply(
            "$path.${property.getMongoFieldName()}"
        )
    }
}