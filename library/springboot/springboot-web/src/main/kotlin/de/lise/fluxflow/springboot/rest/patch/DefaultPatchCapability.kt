package de.lise.fluxflow.springboot.rest.patch

data class DefaultPatchCapability<TElement>(
    override val description: String,
    private val action: (
        original: TElement,
        op: JsonPatchOperation
    ) -> PatchAction<TElement>?
) : PatchCapability<TElement> {
    override fun prepare(
        original: TElement,
        op: JsonPatchOperation
    ): PatchAction<TElement>? {
        return action(
            original,
            op
        )
    }
}