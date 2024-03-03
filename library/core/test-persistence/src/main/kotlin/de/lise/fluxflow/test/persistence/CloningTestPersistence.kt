package de.lise.fluxflow.test.persistence

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule


class CloningTestPersistence<TKey, TObject>(
    private val objectMapper: ObjectMapper = ObjectMapper()
        .activateDefaultTyping(
            AnyPolymorphicTypeValidator(),
            DefaultTyping.EVERYTHING,
            JsonTypeInfo.As.PROPERTY
        )
        .registerModule(ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
        .registerModule(KotlinModule.Builder().build())
        .registerModule(JavaTimeModule())
        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
) {
    private val internalStorage = mutableMapOf<TKey, TObject>()

    operator fun get(key: TKey): TObject? {
        return internalStorage[key]?.let { clone(it) }
    }

    operator fun set(key: TKey, value: TObject) {
        internalStorage[key] = clone(value)
    }

    fun remove(key: TKey) {
        internalStorage.remove(key)
    }

    fun all(): List<TObject> {
        return internalStorage.values.map { clone(it) }
    }

    private fun clone(
        element: TObject
    ): TObject {
        if (element == null) {
            return element
        }

        val type = element.let { it::class.java }
        val json = objectMapper.writeValueAsString(element)
        return objectMapper.readValue(json, type)
    }
}