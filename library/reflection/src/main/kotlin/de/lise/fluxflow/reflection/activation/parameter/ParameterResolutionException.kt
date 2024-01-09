package de.lise.fluxflow.reflection.activation.parameter

class ParameterResolutionException : Exception {
    constructor(message: String) : super(message)
    constructor(param: FunctionParameter<*>, message: String) : this(
        "Resolution failed for parameter \"${param.param.name}\" of function \"${param.function.name}\": ${message}"
    )
}