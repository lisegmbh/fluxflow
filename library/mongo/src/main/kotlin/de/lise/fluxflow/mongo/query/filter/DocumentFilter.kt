package de.lise.fluxflow.mongo.query.filter

import org.springframework.data.mongodb.core.query.Criteria

interface DocumentFilter<TModel> {
    fun toCriteria(): Criteria
}