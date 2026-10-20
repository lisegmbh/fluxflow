package de.lise.fluxflow.mongo.flowquery.expression.compilation.token

import org.bson.Document
import org.slf4j.LoggerFactory
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

internal class RegexToken(
    private val value: StatementToken,
    private val pattern: StatementToken,
    private val ignoreCasing: Boolean = true,
    private val validatePattern: Boolean = true
) : ExpressionToken {

    private val logger = LoggerFactory.getLogger(RegexToken::class.java)

    override fun toExpression(): Document {
        val patternValue = pattern.toStatement()

        if (validatePattern) {
            validateRegexPattern(patternValue)
        }

        val options = buildString {
            if (ignoreCasing) append("i")
        }

        if (logger.isTraceEnabled) {
            logger.trace("Creating regex expression: field='{}', pattern='{}', options='{}'",
                value.toStatement(), patternValue, options)
        }

        return Document(
            value.toStatement(),
            Document(
                mapOf(
                    "\$regex" to patternValue,
                    "\$options" to options
                )
            )
        )
    }

    private fun validateRegexPattern(pattern: String) {
        try {
            Pattern.compile(pattern)
            logger.trace("Regex pattern validation successful: {}", pattern)
        } catch (e: PatternSyntaxException) {
            logger.warn("Invalid regex pattern detected: {} - Error: {}", pattern, e.message)
            throw IllegalArgumentException(
                "Invalid regex pattern '$pattern': ${e.message}", e
            )
        }
    }

    companion object {
        /**
         * Creates a regex token for "starts with" matching
         */
        fun startsWith(
            value: StatementToken,
            prefix: StatementToken,
            ignoreCasing: Boolean = true
        ): RegexToken {
            val escapedPattern = ConvertingStatementToken(prefix) { prefixValue ->
                "^${Pattern.quote(prefixValue)}.*"
            }
            return RegexToken(value, escapedPattern, ignoreCasing)
        }

        /**
         * Creates a regex token for "ends with" matching
         */
        fun endsWith(
            value: StatementToken,
            suffix: StatementToken,
            ignoreCasing: Boolean = true
        ): RegexToken {
            val escapedPattern = ConvertingStatementToken(suffix) { suffixValue ->
                ".*${Pattern.quote(suffixValue)}$"
            }
            return RegexToken(value, escapedPattern, ignoreCasing)
        }

        /**
         * Creates a regex token for "contains" matching
         */
        fun contains(
            value: StatementToken,
            substring: StatementToken,
            ignoreCasing: Boolean = true
        ): RegexToken {
            val escapedPattern = ConvertingStatementToken(substring) { substringValue ->
                ".*${Pattern.quote(substringValue)}.*"
            }
            return RegexToken(value, escapedPattern, ignoreCasing)
        }

        /**
         * Creates a regex token for exact matching (useful for case-insensitive exact matches)
         */
        fun exactMatch(
            value: StatementToken,
            exactValue: StatementToken,
            ignoreCasing: Boolean = false
        ): RegexToken {
            val escapedPattern = ConvertingStatementToken(exactValue) { exact ->
                "^${Pattern.quote(exact)}$"
            }
            return RegexToken(value, escapedPattern, ignoreCasing)
        }

        /**
         * Creates a custom regex token with the given pattern
         */
        fun custom(
            value: StatementToken,
            regexPattern: String,
            ignoreCasing: Boolean = true,
            validatePattern: Boolean = true
        ): RegexToken {
            val patternToken = SimpleStatementTokenImpl(regexPattern)
            return RegexToken(value, patternToken, ignoreCasing, validatePattern)
        }
    }
}
