package de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker

import de.deutschepost.sdm.cdlib.names.Names
import de.deutschepost.sdm.cdlib.release.report.internal.*
import de.deutschepost.sdm.cdlib.utils.resolveEnvByName
import java.io.File
import java.time.ZonedDateTime

data class OslcDependencyLicenseEntry(
    val dependencyName: String,
    val dependencyVersion: String,
    val dependencySources: List<String>,
    val licenses: List<OslcLicenseEntry>,
    val isManuallyApproved: Boolean = false,
    val isOverwritten: Boolean = false
) {
    companion object
}

fun List<OslcDependencyLicenseEntry>.transformWithAcceptList(acceptListFile: File): List<OslcDependencyLicenseEntry> {
    val acceptList = OslcComplianceChecker.readAcceptList(acceptListFile)
    return map { entry ->
        val acceptEntry = acceptList.findMatchingAcceptEntry(entry)
        when {
            acceptEntry?.newLicense != null -> entry.copy(
                licenses = listOf(acceptEntry.newLicense),
                isOverwritten = true
            )

            acceptEntry != null -> entry.copy(isManuallyApproved = true, isOverwritten = true)
            else -> entry
        }
    }
}

data class OslcLicenseEntry(
    val license: String,
    val url: String,
)

data class OslcTestPreResult(
    override val tool: Tool,
    override val uri: String,
    override val date: ZonedDateTime = ZonedDateTime.now(),
    override val reportType: ReportType = ReportType.OSLC_PRE,
    val dependencyLicenseEntries: List<OslcDependencyLicenseEntry>
) : TestResult() {
    companion object
}

fun OslcTestResult.Companion.from(
    result: OslcTestPreResult,
    isDistribution: Boolean,
    acceptListFile: File? = null
): OslcTestResult {
    val licenseEntries = result.dependencyLicenseEntries
    val transformedEntries = acceptListFile?.let {
        logger.info { "Found AcceptListFile: ${acceptListFile.absoluteFile}" }
        licenseEntries.transformWithAcceptList(acceptListFile)
    } ?: licenseEntries
    val overwrittenEntries = transformedEntries.filter { it.isOverwritten }
    val numOfOverwrittenLicenses = overwrittenEntries.size
    logger.info { "OslcComplianceChecker has $numOfOverwrittenLicenses overwritten licenses" }

    val dependenciesWithIncompliantLicenses =
        OslcComplianceChecker.getIncomliantLicenses(transformedEntries, isDistribution)

    val allLicenses = OslcComplianceChecker.getAllLicensesNames(transformedEntries, isDistribution)

    return OslcTestResult(
        tool = result.tool,
        uri = result.uri,
        reportType = ReportType.OSLC,
        uniqueLicenses = allLicenses.distinct(),
        projectName = resolveEnvByName(name = Names.CDLIB_APP_NAME),
        projectId = -1,
        depth = "libs",
        policyProfile = if (isDistribution) PROFILE_DISTRIBUTION else PROFILE_NON_DISTRIBUTION,
        unapprovedItems = dependenciesWithIncompliantLicenses.mapKeys { entry -> entry.key.license }
            .mapValues { mapEntry ->
                mapOf(
                    "REJECTED" to
                        mapEntry.value.map { entry -> entry.dependencyName }
                )
            },
        totalArtifactCount = allLicenses.size,
        acceptedItems = overwrittenEntries.map { it.dependencyName },
        complianceStatus = if (dependenciesWithIncompliantLicenses.isNotEmpty()) OslcComplianceStatus.RED else OslcComplianceStatus.GREEN
    )
}
