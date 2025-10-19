package de.fluxflow.flowquery.expression

enum class BinaryOperation(
    val symbol: String,
) {
    // Add("+"),
    // Subtract("-"),
    // Multiply("*"),
    // Divide("/"),
    Equal("="),
    NotEqual("!="),
    GreaterThan(">"),
    LessThan("<"),
    GreaterThanOrEqual(">="),
    LessThanOrEqual("<=")
}