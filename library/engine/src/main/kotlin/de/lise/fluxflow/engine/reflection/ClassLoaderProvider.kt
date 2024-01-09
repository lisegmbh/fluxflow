package de.lise.fluxflow.engine.reflection

fun interface ClassLoaderProvider {
    fun provide(): ClassLoader
}