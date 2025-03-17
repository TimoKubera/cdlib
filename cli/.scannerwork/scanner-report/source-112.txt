package de.deutschepost.sdm.cdlib.release.report.external


import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import de.deutschepost.sdm.cdlib.release.report.internal.*
import java.time.ZonedDateTime

data class OdcTestResult(
    val dependencies: List<OdcDependency> = emptyList(),
    val projectInfo: ProjectInfo,
    val scanInfo: ScanInfo = ScanInfo()
) {
    data class OdcDependency(
        val description: String = "",
        val fileName: String = "",
        val filePath: String = "",
        val sha256: String = "",
        val suppressedVulnerabilities: List<OdcVulnerability> = emptyList(),
        val vulnerabilities: List<OdcVulnerability> = emptyList(),
        val vulnerabilityIds: List<VulnerabilityId> = emptyList()
    ) {
        data class OdcVulnerability(
            val cvssv2: Score = Score(""),
            val cvssv3: Score = Score(""),
            val severity: String = "",
            val description: String = "",
            val name: String = "",
        ) {
            val internalSeverity: Severity = when {
                severity.isNotBlank() -> Severity.resilientValueOf(severity.uppercase())
                cvssv3.severity.isNotBlank() -> Severity.resilientValueOf(cvssv3.severity.uppercase())
                cvssv2.severity.isNotBlank() -> Severity.resilientValueOf(cvssv2.severity.uppercase())
                else -> Severity.UNKNOWN
            }

            data class Score(
                @JsonProperty("severity")
                @JsonAlias("baseSeverity")
                val severity: String,
            )
        }

        data class VulnerabilityId(
            val confidence: String = "",
            val id: String = "",
            val url: String = ""
        )

        private fun OdcVulnerability.toVulnerability(): Vulnerability {
            return Vulnerability(
                id = this.name,
                severity = this.internalSeverity,
                description = this.description,
                origin = fileName,
                additionalInfo = mapOf("cpe" to vulnerabilityIds.map { vulnId ->
                    vulnId.id
                })
            )
        }

        fun getOpenVulnerabilityList(): List<Vulnerability> {
            return vulnerabilities.map { it.toVulnerability() }
        }

        fun getSuppressedVulnerabilityList(): List<Vulnerability> {
            return suppressedVulnerabilities.map { it.toVulnerability() }
        }
    }

    data class ProjectInfo(
        val groupID: String = "",
        val name: String = "",
        val reportDate: ZonedDateTime,
        val version: String = ""
    )

    data class ScanInfo(
        val dataSource: List<DataSource> = emptyList(),
        val engineVersion: String = ""
    ) {
        data class DataSource(
            val name: String = "",
            val timestamp: String = ""
        )
    }
}

fun SecurityTestResult.Companion.from(odcTestResult: OdcTestResult, uri: String) =
    SecurityTestResult(
        uri = uri,
        date = odcTestResult.projectInfo.reportDate,
        reportType = ReportType.SCA,
        tool = Tool(
            name = "OWASP Dependency Check",
            version = odcTestResult.scanInfo.engineVersion,
            ruleVersion = odcTestResult.scanInfo.dataSource.first {
                it.name in listOf("NVD API Last Modified", "NVD CVE Modified")
            }.timestamp,
            vendor = "OWASP Dependency Check"

        ),
        vulnerabilities = Vulnerabilities(
            scannedObjects = odcTestResult.dependencies.size,
            vulnerableObjects = odcTestResult.dependencies.count {
                it.vulnerabilities.isNotEmpty()
            },
            open = odcTestResult.dependencies.flatMap {
                it.getOpenVulnerabilityList()
            },
            suppressed = odcTestResult.dependencies.flatMap {
                it.getSuppressedVulnerabilityList()
            }
        )
    )



