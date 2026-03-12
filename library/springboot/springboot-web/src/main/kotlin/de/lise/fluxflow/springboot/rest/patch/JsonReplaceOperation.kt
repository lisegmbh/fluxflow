package de.lise.fluxflow.springboot.rest.patch

import com.fasterxml.jackson.databind.JsonNode

data class JsonReplaceOperation(
    override val path: String,
    val value: JsonNode,
) : JsonPatchOperation {
    override val op: String
        get() = "replace"
}