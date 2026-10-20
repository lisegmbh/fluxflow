package de.lise.fluxflow.mongo.flowquery.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class UnpagedMongoResults<T : Any>(
 override val elements: List<T>,
) : MongoExecutionResults<T> {
    override val page: Page<T>
        get() = PageableExecutionUtils.getPage(
            elements.toList(),
            Pageable.unpaged(),
        ) {
            elements.size.toLong()
        }
}