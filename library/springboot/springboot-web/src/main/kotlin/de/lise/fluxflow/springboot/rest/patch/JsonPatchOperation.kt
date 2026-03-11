package de.lise.fluxflow.springboot.rest.patch

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonSubTypes(
    JsonSubTypes.Type(JsonReplaceOperation::class)
)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.DEDUCTION
)
interface JsonPatchOperation {
    val op: String
}

