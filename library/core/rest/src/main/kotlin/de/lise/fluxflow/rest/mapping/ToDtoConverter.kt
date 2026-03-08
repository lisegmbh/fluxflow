package de.lise.fluxflow.rest.mapping

fun interface ToDtoConverter<
        in TInput,
        out TOutput
> {
    fun toDto(
        input: TInput
    ): TOutput
}