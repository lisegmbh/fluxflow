package de.lise.fluxflow.mongo.query.filter

import de.lise.fluxflow.query.filter.StartsWithFilter
import org.springframework.data.mongodb.core.query.Criteria
import java.util.regex.Pattern

data class MongoStartsWithFilter(
    private val prefix: String,
    private val ignoreCasing: Boolean
) : MongoFilter<String> {

    constructor(startsWithFilter: StartsWithFilter) : this(startsWithFilter.prefix, startsWithFilter.ignoreCasing)

    override fun apply(path: String): Criteria {
        return Criteria.where(path).regex(
            "${Pattern.quote(prefix)}.*",
            if (ignoreCasing) MongoFilter.IgnoreStringCasingOption else null
        )
    }
}