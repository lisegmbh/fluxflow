package de.lise.fluxflow.springboot.rest.patch

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class JsonPatch @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    @JsonValue
    val operations: List<JsonPatchOperation>
)