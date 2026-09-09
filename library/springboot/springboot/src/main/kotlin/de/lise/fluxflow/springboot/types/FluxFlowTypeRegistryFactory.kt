package de.lise.fluxflow.springboot.types

import de.lise.fluxflow.reflection.types.TypeManifestEntry
import de.lise.fluxflow.reflection.types.TypeRegistry
import org.springframework.context.ApplicationContext

/**
 * Builds the complete trusted type registry for one application context.
 */
class FluxFlowTypeRegistryFactory(
    private val context: ApplicationContext,
    private val classLoader: ClassLoader,
    contributors: Map<String, TypeRegistrationContributor> = emptyMap(),
) {
    private val contributors = contributors.toMap()

    constructor(
        context: ApplicationContext,
        classLoader: ClassLoader,
        contributors: List<TypeRegistrationContributor>,
    ) : this(
        context,
        classLoader,
        contributors.mapIndexed { index, contributor ->
            "contributor[$index]" to contributor
        }.toMap(),
    )

    fun create(): TypeRegistry {
        val scanRoots = SpringScanRootResolver(context).resolve()
        val scannedEntries = AnnotatedTypeRegistrationScanner(classLoader).scan(scanRoots)
        val explicitEntries = contributors.toSortedMap().flatMap { (beanName, contributor) ->
            contributor.registrations().map { registration ->
                TypeManifestEntry(
                    registration.role,
                    registration.key,
                    registration.type.java.name,
                    "explicit contributor '$beanName' (${contributor.javaClass.name})",
                )
            }
        }
        return TypeRegistry.load(classLoader, scannedEntries + explicitEntries)
    }
}
