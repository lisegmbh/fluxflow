package de.lise.fluxflow.mongo.docker

import org.springframework.context.annotation.Conditional

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Conditional(DockerAvailableCondition::class)
annotation class ConditionalOnDocker
