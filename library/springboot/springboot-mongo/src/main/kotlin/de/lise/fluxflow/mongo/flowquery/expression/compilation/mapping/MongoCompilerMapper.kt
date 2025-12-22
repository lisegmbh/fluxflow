package de.lise.fluxflow.mongo.flowquery.expression.compilation.mapping

import de.fluxflow.flowquery.expression.Expression

/**
 * Maps expressions before they are compiled into MongoDB tokens.
 * 
 * This mapper runs as a preprocessing step in the MongoDB compilation pipeline,
 * transforming expressions to ensure they are compatible with MongoDB's query model.
 * For example, it may convert certain value types (like String IDs to ObjectIds) or
 * adjust property references to match MongoDB field names.
 * 
 * The mapper is applied in [de.lise.fluxflow.mongo.flowquery.expression.compilation.MongoCompiler]
 * before the actual token compilation occurs.
 * 
 * @see de.lise.fluxflow.mongo.flowquery.expression.compilation.MongoCompiler
 * @see ObjectIdReplacer
 */
interface MongoCompilerMapper {
    /**
     * Maps the given expression to a MongoDB-compatible form.
     * 
     * This method transforms the expression tree to ensure compatibility with MongoDB's
     * query capabilities. Implementations may modify value types, property references,
     * or expression structure as needed.
     * 
     * @param TRoot The root type of the expression tree
     * @param TResult The result type of the expression
     * @param expression The expression to map
     * @return The mapped expression, maintaining the same type parameters
     */
    fun <TRoot, TResult> map(expression: Expression<TRoot, TResult>): Expression<TRoot, TResult>
}