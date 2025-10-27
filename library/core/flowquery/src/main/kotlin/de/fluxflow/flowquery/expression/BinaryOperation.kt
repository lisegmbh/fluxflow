package de.fluxflow.flowquery.expression

enum class BinaryOperation(
    val symbol: String,
) {
    Equal("="),
    NotEqual("!="),
    GreaterThan(">"),
    LessThan("<"),
    GreaterThanOrEqual(">="),
    LessThanOrEqual("<=")
}