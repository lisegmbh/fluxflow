package de.lise.fluxflow.stereotyped.unwrapping

class IllegalUnwrapException(
    message: String,
    val elementToBeUnwrapped: Any?
) : Exception(message)