package de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker

data class OslcAcceptList(
    val entries: List<OslcLicenseAcceptEntry>
) {
    fun findMatchingAcceptEntry(dependencyEntry: OslcDependencyLicenseEntry): OslcLicenseAcceptEntry? =
        entries.firstOrNull {
            it.doesAcceptEntryMatchDependencyEntry(dependencyEntry)
        }
}

data class OslcLicenseAcceptEntry(
    val packageName: String,
    val justification: String,
    val version: String,
    val newLicense: OslcLicenseEntry?
) {
    fun doesAcceptEntryMatchDependencyEntry(dependencyEntry: OslcDependencyLicenseEntry): Boolean =
        if (packageName.contentEquals(dependencyEntry.dependencyName, true)) {
            val depVersion = dependencyEntry.dependencyVersion
            VersionSpecification.fromString(depVersion) in VersionSpecification.rangeFromString(version)
        } else {
            false
        }

}
