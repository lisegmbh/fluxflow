package de.lise.fluxflow.rest.mapping

import java.lang.reflect.Type

fun interface FromDtoConverter<TSource> {
    fun fromDto(
        value: TSource,
        to: Type
    ): Any?
}