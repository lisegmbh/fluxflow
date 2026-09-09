package de.lise.fluxflow.reflection.types

/**
 * Finds every FluxFlow type manifest visible to an application class loader.
 */
class TypeManifestLoader(
    private val classLoader: ClassLoader,
) {
    fun load(): List<TypeManifestEntry> {
        val resources = try {
            classLoader.getResources(TypeManifest.RESOURCE_PATH).toList()
        } catch (exception: Exception) {
            throw TypeManifestException(
                "Could not enumerate FluxFlow type manifests at '${TypeManifest.RESOURCE_PATH}'.",
                exception,
            )
        }
        return resources
        .sortedBy { it.toExternalForm() }
        .flatMap { resource ->
            try {
                resource.openStream().bufferedReader(Charsets.UTF_8).use { reader ->
                    TypeManifest.read(resource.toExternalForm(), reader)
                }
            } catch (exception: TypeManifestException) {
                throw exception
            } catch (exception: Exception) {
                throw TypeManifestException(
                    "Could not read FluxFlow type manifest '${resource.toExternalForm()}'.",
                    exception,
                )
            }
        }
    }
}
