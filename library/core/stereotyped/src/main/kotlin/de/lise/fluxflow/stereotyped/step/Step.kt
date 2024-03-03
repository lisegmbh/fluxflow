package de.lise.fluxflow.stereotyped.step

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class Step(
    val kind: String = ""
)
