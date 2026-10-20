package de.lise.fluxflow.mongo.docker

import org.springframework.context.annotation.Conditional

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Conditional(DockerAvailableCondition::class)
annotation class ConditionalOnDocker()