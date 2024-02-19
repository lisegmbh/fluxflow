package de.lise.fluxflow.springboot.expression

import de.lise.fluxflow.stereotyped.workflow.SelectorExpression
import de.lise.fluxflow.stereotyped.workflow.SelectorExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext

class SpringSelectorExpressionParser(
    private val spelParser: SpelExpressionParser
) : SelectorExpressionParser {
    override fun parse(expression: String): SelectorExpression {
        val springExpression = spelParser.parseExpression(expression)
        return SelectorExpression{ old, new, current ->
            val context = StandardEvaluationContext(current)
            context.setVariable("old", old)
            context.setVariable("original", old)
            context.setVariable("new", new)
            context.setVariable("updated", new)
            context.setVariable("current", current)

            springExpression.getValue(context)
        }
    }
}