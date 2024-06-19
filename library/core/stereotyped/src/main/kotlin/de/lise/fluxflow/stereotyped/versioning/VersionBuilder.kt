package de.lise.fluxflow.stereotyped.versioning

import de.lise.fluxflow.api.versioning.NoVersion
import de.lise.fluxflow.api.versioning.Version
import de.lise.fluxflow.api.versioning.VersionWithCompatibility
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

private typealias VersionAnnotation = de.lise.fluxflow.stereotyped.versioning.Version

class VersionBuilder(
    private val cache: MutableMap<KClass<*>, Version> = mutableMapOf()
) {
    fun build(type: KClass<*>): Version {
        return cache.getOrPut(type) {
            type.findAnnotation<VersionAnnotation>()?.let { annotation ->
                if(annotation.compatibleVersions.isEmpty()) {
                    Version.parse(annotation.value)
                } else {
                    VersionWithCompatibility(
                        Version.parse(annotation.value),
                        annotation.compatibleVersions.map { compatibleVersion -> Version.parse(compatibleVersion) }
                    )
                }
            } ?: NoVersion()
        }
    }
}