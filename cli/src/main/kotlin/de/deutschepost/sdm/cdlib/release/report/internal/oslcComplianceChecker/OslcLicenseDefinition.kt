package de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker

import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.OslcComplianceChecker.containsIgnoreCase

data class OslcLicenseDefinition(
    val license: String,
    val nameAliases: List<String>,
    val urlAliases: List<String>,
    val isCompliant: Boolean,
) {
    fun isOslcLicenseEntryMatching(licenseEntry: OslcLicenseEntry): Boolean =
        licenseEntry.license.containsIgnoreCase(license) ||
            nameAliases.any { nameAlias -> licenseEntry.license.containsIgnoreCase(nameAlias) } ||
            urlAliases.any { urlAlias -> licenseEntry.url.containsIgnoreCase(urlAlias) }

}
