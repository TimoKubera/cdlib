package de.deutschepost.sdm.cdlib.release.report.external

import com.fasterxml.jackson.annotation.JsonFormat
import de.deutschepost.sdm.cdlib.release.report.internal.OsclLicenseReportEntry
import de.deutschepost.sdm.cdlib.release.report.internal.OslcComplianceStatus
import de.deutschepost.sdm.cdlib.release.report.internal.OslcTestResult
import de.deutschepost.sdm.cdlib.release.report.internal.Tool
import java.time.ZonedDateTime


data class OslcMavenPluginTestResult(
    val projectName: String,
    val artifactId: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss O")
    val creationDate: ZonedDateTime,
    val complianceStatus: String,
    val pluginVersion: String,
    val incompliantLicenses: List<OsclLicenseReportEntry>,
    val licenses: List<OsclLicenseReportEntry>
)

fun OslcTestResult.Companion.from(
    oslcMavenPluginTestResult: OslcMavenPluginTestResult,
    uri: String
): OslcTestResult {

    return OslcTestResult(
        tool = Tool(
            name = Tool.OSLC_MAVEN_PLUGIN_NAME,
            version = oslcMavenPluginTestResult.pluginVersion,
            vendor = "",
            ruleVersion = ""
        ),
        uri = uri,
        uniqueLicenses = oslcMavenPluginTestResult.licenses.map { item ->
            item.license
        }.distinct().sorted(),
        projectName = oslcMavenPluginTestResult.projectName,
        projectId = -1,
        depth = "libs",
        policyProfile = PROFILE_PLUGIN,
        unapprovedItems = oslcMavenPluginTestResult.incompliantLicenses.groupBy { license ->
            license.license
        mapOf("REJECTED" to it.value.map { it.license })
        },
        totalArtifactCount = oslcMavenPluginTestResult.licenses.sumOf { it.count },
        complianceStatus = when (oslcMavenPluginTestResult.complianceStatus) {
            "APPROVED" -> OslcComplianceStatus.GREEN
            "REJECTED" -> OslcComplianceStatus.RED
            else -> OslcComplianceStatus.RED
        }
    )
}

