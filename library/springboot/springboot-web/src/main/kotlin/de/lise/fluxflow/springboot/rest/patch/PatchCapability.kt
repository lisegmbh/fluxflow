package de.lise.fluxflow.springboot.rest.patch

import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

interface PatchCapability<TElement> {
    val description: String
    fun prepare(
        original: TElement,
        op: JsonPatchOperation
    ): PatchAction<TElement>?

    companion object {
        fun <TElement> withDescription(description: String): Builder<TElement, JsonPatchOperation> {
            return Builder<TElement, JsonPatchOperation>(
                description = description,
                condition = { true },
                mapper = { it }
            )
        }
    }

    data class Builder<TElement, TPatch : JsonPatchOperation>(
        private val description: String,
        private val condition: (patch: TPatch) -> Boolean,
        private val mapper: (patch: JsonPatchOperation) -> TPatch
    ) {

        inline fun <reified TNewPatch : JsonPatchOperation> forOperationType(): Builder<TElement, TNewPatch> {
            return forOperationType(TNewPatch::class)
        }

        fun <TNewPatch : JsonPatchOperation> forOperationType(
            type: KClass<TNewPatch>
        ): Builder<TElement, TNewPatch> {
            return Builder(
                description = description,
                condition = {
                    this.condition(mapper(it)) && type.isSuperclassOf(it::class)
                },
                mapper = {
                    mapper(it) as TNewPatch
                }
            )
        }

        fun build(
            then: (original: TElement, patch: TPatch) -> PatchAction<TElement>
        ): PatchCapability<TElement> {
            return DefaultPatchCapability(description) { original, op ->
                when (condition(mapper(op))) {
                    true -> then(original, mapper(op))
                    false -> null
                }
            }
        }

        companion object {
            fun <TElement> Builder<TElement, JsonReplaceOperation>.forPath(
                path: String
            ): Builder<TElement, JsonReplaceOperation> {
                return Builder(
                    description = description,
                    condition = {
                        this.condition(it) && it.path == path
                    },
                    mapper = mapper
                )
            }
        }
    }
}



