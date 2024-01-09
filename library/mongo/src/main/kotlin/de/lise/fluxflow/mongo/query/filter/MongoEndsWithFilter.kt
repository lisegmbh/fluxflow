package de.lise.fluxflow.mongo.query.filter

import de.lise.fluxflow.query.filter.EndsWithFilter
import org.springframework.data.mongodb.core.query.Criteria
import java.util.regex.Pattern

data class MongoEndsWithFilter(
    private val suffix: String,
    private val ignoreCasing: Boolean
) : MongoFilter<String> {
    constructor(endsWithFilter: EndsWithFilter) : this(endsWithFilter.suffix, endsWithFilter.ignoreCasing)

    override fun apply(path: String): Criteria {
        return Criteria.where(path).regex(
            ".*${Pattern.quote(suffix)}",
            if (ignoreCasing) MongoFilter.IgnoreStringCasingOption else null
        )
    }
}