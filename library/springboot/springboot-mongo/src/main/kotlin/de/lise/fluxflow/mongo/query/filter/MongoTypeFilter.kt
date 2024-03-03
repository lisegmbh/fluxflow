package de.lise.fluxflow.mongo.query.filter

import org.springframework.data.mongodb.core.query.Criteria
import kotlin.reflect.KClass

class MongoTypeFilter<TModel>(
    val type: KClass<*>,
) : MongoFilter<TModel> {
    override fun apply(path: String): Criteria {
        val classTypePath = if (path.isEmpty()) {
            "_class"
        } else {
            "${path}._class"
        }

        return Criteria.where(classTypePath).`is`(type.qualifiedName)
    }
}
