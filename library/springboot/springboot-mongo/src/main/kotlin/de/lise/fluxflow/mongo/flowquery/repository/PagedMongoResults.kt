package de.lise.fluxflow.mongo.flowquery.repository

import org.springframework.data.domain.Page

class PagedMongoResults<T : Any>(
    override val page: Page<T>
) : MongoExecutionResults<T> {
    override val elements: Iterable<T>
        get() {
            return page
        }
}