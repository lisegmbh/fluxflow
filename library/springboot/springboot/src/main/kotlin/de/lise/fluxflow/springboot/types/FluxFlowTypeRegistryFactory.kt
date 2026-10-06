package de.lise.fluxflow.springboot.types

import de.lise.fluxflow.reflection.types.TypeManifestLoader
import de.lise.fluxflow.reflection.types.TypeRegistryEntry
import de.lise.fluxflow.reflection.types.TypeRegistry
import org.springframework.context.ApplicationContext

/**
 * Builds the complete trusted type registry for one application context.
 */
class FluxFlowTypeRegistryFactory(
    private val context: ApplicationContext,
    private val classLoader: ClassLoader,
    contributors: Map<String, TypeRegistrationContributor> = context.getBeansOfType(TypeRegistrationContributor::class.java),
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
        val contents = TypeManifestLoader(classLoader).loadContents()
        val scanRoots = SpringScanRootResolver(context).resolve()
        val scannedEntries = AnnotatedTypeRegistrationScanner(classLoader)
            .scan(scanRoots, contents.coveredPackages)
        val explicitEntries = contributors.toSortedMap().flatMap { (beanName, contributor) ->
            contributor.registrations().map { registration ->
                TypeRegistryEntry(
                    registration.role,
                    registration.key,
                    registration.type.java.name,
                    registration.type,
                    listOf("explicit contributor '$beanName' (${contributor.javaClass.name})"),
                )
            }
        }
        return TypeRegistry.create(classLoader, contents.entries + scannedEntries, explicitEntries)
    }
}
