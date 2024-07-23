package de.lise.fluxflow.api.versioning

/**
 * A [CompatibilityTester] is responsible to test if a version is compatible to another version.  
 */
interface CompatibilityTester {
    /**
     * Checks if the potentially updated [newVersion] is compatible to an [oldVersion] baseline.
     * 
     * Note that the order of operands is important, 
     * as an update from A to B might be compatible, 
     * while a downgrade from B to A might not be.
     * @return the compatibility result.
     */
    fun checkCompatibility(oldVersion: Version, newVersion: Version): VersionCompatibility
}