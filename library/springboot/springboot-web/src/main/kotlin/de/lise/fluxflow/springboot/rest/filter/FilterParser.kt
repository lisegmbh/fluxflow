package de.lise.fluxflow.springboot.rest.filter

import de.fluxflow.flowquery.expression.Expression
import de.lise.fluxflow.springboot.odata.filter.grammar.ODataFilterLexer
import de.lise.fluxflow.springboot.odata.filter.grammar.ODataFilterParser
import org.antlr.v4.runtime.*
import kotlin.reflect.KClass

class FilterParser<TElement : Any>(
    private val elementKind: KClass<TElement>,
    private val treeBuilder: ODataTreeBuilder<TElement> = ODataTreeBuilder(elementKind),
    private val errorListener: ANTLRErrorListener = object : BaseErrorListener() {
        override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String?,
            e: RecognitionException?
        ) {
            throw ExpressionParsingException(
                "Failed to parse symbol at line $line, column $charPositionInLine: $msg"
            )
        }
    }
) {

    fun parse(value: String): Expression<TElement, *> {
        val lexer = ODataFilterLexer(
            CharStreams.fromString(value)
        )

        lexer.removeErrorListeners()
        lexer.addErrorListener(errorListener)

        val tokens = CommonTokenStream(lexer)


        return parse(tokens)
    }

    private fun parse(
        tokens: CommonTokenStream
    ): Expression<TElement, *> {
        val parser = ODataFilterParser(tokens)
        parser.removeErrorListeners()
        parser.addErrorListener(errorListener)

        val tree = parser.filter()

        return treeBuilder.build(tree)
    }
}