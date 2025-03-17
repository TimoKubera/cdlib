package de.deutschepost.sdm.cdlib.release.report.external


import com.fasterxml.jackson.annotation.JsonProperty
import de.deutschepost.sdm.cdlib.release.report.external.cca.CcaSuppressionList
import de.deutschepost.sdm.cdlib.release.report.internal.*
import java.time.ZonedDateTime

data class CcaTestResult(
    @JsonProperty("application/vnd.security.vulnerability.report; version=1.1")
    val ccaReport: CcaReport
) {
    data class CcaReport(
        @JsonProperty("generated_at")
        val generatedAt: ZonedDateTime,
        @JsonProperty("scanner")
        val scanner: Scanner,
        @JsonProperty("severity")
        val severity: String,
        @JsonProperty("vulnerabilities")
        val vulnerabilities: List<CcaVulnerability>
    ) {
        data class Scanner(
            @JsonProperty("name")
            val name: String,
            @JsonProperty("vendor")
            val vendor: String,
            @JsonProperty("version")
            val version: String
        )

        data class CcaVulnerability(
            @JsonProperty("artifact_digests")
            val artifactDigests: List<String>,
            @JsonProperty("cwe_ids")
            val cweIds: List<String>,
            @JsonProperty("description")
            val description: String,
            @JsonProperty("fix_version")
            val fixVersion: String,
            @JsonProperty("id")
            val id: String,
            @JsonProperty("links")
            val links: List<String>,
            @JsonProperty("package")
            val packageX: String,
            @JsonProperty("severity")
            val severity: String?,
            @JsonProperty("version")
            val version: String
        )
    }

    fun getTool() =
        with(ccaReport.scanner) {
            Tool(
                name = name,
                version = version,
                vendor = vendor,
                ruleVersion = ""
            )
        }

    private fun CcaReport.CcaVulnerability.toVulnerability(): Vulnerability =
        Vulnerability(
            id = id,
            severity = if (this.severity.isNullOrBlank()) Severity.UNKNOWN else Severity.resilientValueOf(this.severity),
            origin = packageX,
            description = description,
            additionalInfo = mapOf(
                CcaReport.CcaVulnerability::cweIds.name to cweIds,
                CcaReport.CcaVulnerability::fixVersion.name to fixVersion,
                CcaReport.CcaVulnerability::links.name to links,
            ),
        )

    val vulnerabilities by lazy {
        ccaReport.vulnerabilities.map { it.toVulnerability() }
    }
}

fun SecurityTestResult.Companion.from(
    ccaTestResult: CcaTestResult,
    ccaSuppressionList: CcaSuppressionList,
    uri: String,
): SecurityTestResult {
    val cveIds = ccaSuppressionList.getCveIds()
    val (suppressedVulnerabilities, openVulnerabilities) = ccaTestResult.vulnerabilities.partition { it.id in cveIds }
    return SecurityTestResult(
        uri = uri,
        date = ccaTestResult.ccaReport.generatedAt,
        reportType = ReportType.CCA,
        tool = ccaTestResult.getTool(),
        vulnerabilities = Vulnerabilities(
            scannedObjects = 1,
            vulnerableObjects = ccaTestResult.vulnerabilities.size,
            open = openVulnerabilities,
            suppressed = suppressedVulnerabilities
        )
    )
}
