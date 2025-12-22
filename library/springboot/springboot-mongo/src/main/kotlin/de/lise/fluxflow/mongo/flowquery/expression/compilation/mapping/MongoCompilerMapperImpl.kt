package de.lise.fluxflow.mongo.flowquery.expression.compilation.mapping

import de.fluxflow.flowquery.expression.Expression

internal class MongoCompilerMapperImpl : MongoCompilerMapper {
    private val mapper = ObjectIdReplacer().toMapper()
    
    override fun <TRoot, TResult> map(expression: Expression<TRoot, TResult>): Expression<TRoot, TResult> {
        @Suppress("UNCHECKED_CAST")
        return mapper.map(expression) as Expression<TRoot, TResult>
    }
}