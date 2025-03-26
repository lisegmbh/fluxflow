package de.lise.fluxflow.mongo.query.filter

import de.lise.fluxflow.mongo.query.getMongoFieldName
import org.springframework.data.mongodb.core.query.Criteria
import kotlin.reflect.KProperty1

data class MongoElemMatchFilter<TObject, TProperty>(
    private val property: KProperty1<TObject, TProperty>,
    private val filterForProperty: MongoFilter<TProperty>
) : MongoFilter<Collection<TObject>> {
    override fun apply(path: String): Criteria {
        return Criteria(path).elemMatch(
            filterForProperty.apply(
                property.getMongoFieldName()
            )
        )
    }
}
