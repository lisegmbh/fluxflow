package de.lise.fluxflow.springboot.rest.patch

fun interface PatchAction<TElement> {
    fun perform(element: TElement): TElement
}