package de.lise.fluxflow.stereotyped

/**
 * This annotation is used to "import" all definition elements from the annotated properties type,
 * as if they were declared here.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Import