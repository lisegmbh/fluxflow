package de.lise.fluxflow.mongo.query.filter

import de.lise.fluxflow.mongo.query.getMongoFieldName
import org.springframework.data.mongodb.core.query.Criteria
import kotlin.reflect.KProperty1

class MongoPropertyFilter<TObject, TProperty>(
    private val property: KProperty1<TObject, TProperty>,
    private val filterForValue: MongoFilter<TProperty>
) : MongoFilter<TObject> {
    override fun apply(path: String): Criteria {
        return filterForValue.apply(
            "${path}.${property.getMongoFieldName()}"
        )
    }
}

