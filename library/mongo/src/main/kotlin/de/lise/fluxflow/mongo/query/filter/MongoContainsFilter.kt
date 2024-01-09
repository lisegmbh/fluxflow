package de.lise.fluxflow.mongo.query.filter

import de.lise.fluxflow.query.filter.ContainsFilter
import org.springframework.data.mongodb.core.query.Criteria
import java.util.regex.Pattern

data class MongoContainsFilter(
    private val content: String,
    private val ignoreCasing: Boolean
) : MongoFilter<String> {
    constructor(containsFilter: ContainsFilter) : this(
        containsFilter.content,
        containsFilter.ignoreCasing
    )

    override fun apply(path: String): Criteria {
        return Criteria.where(path).regex(
            ".*${Pattern.quote(content)}.*",
            if (ignoreCasing) MongoFilter.IgnoreStringCasingOption else null
        )
    }
}