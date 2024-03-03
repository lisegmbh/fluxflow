package de.lise.fluxflow.mongo

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.AliasFor

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
@ConditionalOnProperty
annotation class ConditionalOnFluxFlowMongo(
    @get:AliasFor(
        annotation = ConditionalOnProperty::class, 
        attribute = "value"
    )
    val value: Array<String> = ["fluxflow.mongo.enabled"],
    
    @get:AliasFor(
        annotation = ConditionalOnProperty::class,
        attribute = "havingValue"
    )
    val havingValue: String = "true",
    
    @get:AliasFor(
        annotation = ConditionalOnProperty::class,
        attribute = "matchIfMissing"
    )
    val matchIfMissing: Boolean = true
)