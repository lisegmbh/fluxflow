package de.lise.fluxflow.query.sort

class IncomparableElementsException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(element1: Any?, element2: Any?) : this(
        "Can not compare \"$element1\" to \"$element2\""
    )

    constructor(element1: Any?, element2: Any?, cause: Throwable) : this(
        "Can not compare \"$element1\" to \"$element2\"",
        cause
    )
}