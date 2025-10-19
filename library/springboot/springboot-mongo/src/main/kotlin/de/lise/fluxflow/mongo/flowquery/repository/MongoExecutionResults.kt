package de.lise.fluxflow.mongo.flowquery.repository

import org.springframework.data.domain.Page

interface MongoExecutionResults<T> {
    val elements: Iterable<T>
    val page: Page<T>
}