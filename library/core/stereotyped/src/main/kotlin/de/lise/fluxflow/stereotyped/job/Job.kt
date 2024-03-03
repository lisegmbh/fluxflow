package de.lise.fluxflow.stereotyped.job

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class Job(
    val kind: String = ""
)
