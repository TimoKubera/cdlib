package de.deutschepost.sdm.cdlib.release.report.external

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import de.deutschepost.sdm.cdlib.release.report.internal.*
import java.time.LocalDateTime
import java.time.ZoneId

data class ZapTestResult(
    @JsonProperty("@generated")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE, d MMM yyyy HH:mm:ss", locale = "ENGLISH")
    val generated: LocalDateTime, // Tue, 12 Oct 2021 06:27:20
    @JsonProperty("site")
    val sites: List<ZapSite> = listOf(),
    @JsonProperty("@version")
    val version: String = "" // D-2021-10-12
) {
    data class ZapSite(
        @JsonProperty("alerts")
        val alerts: List<ZapAlert> = listOf(),
        @JsonProperty("@host")
        val host: String = "", // its-abq-showcase-uat-front-end-master.cz3.cloudapps.dhl.com
        @JsonProperty("@name")
        val name: String = "", // https://its-abq-showcase-uat-front-end-master.cz3.cloudapps.dhl.com
        @JsonProperty("@port")
        val port: String = "", // 443
        @JsonProperty("@ssl")
        val ssl: String = "" // true
    ) {
        data class ZapAlert(
            val alert: String = "", // Information Disclosure - Suspicious Comments
            val alertRef: String = "", // 10027
            val count: Int = 0, // 13
            val cweid: String = "", // 200
            val desc: String = "", // <p>The response appears to contain suspicious comments which may help an attacker. Note: Matches made within script blocks or files are against the entire content not only comments.</p>
            val instances: List<ZapInstance> = listOf(),
            val name: String = "", // Information Disclosure - Suspicious Comments
            val pluginid: String = "", // 10027
            val riskcode: Int = 0, // Informational (Low)
            val confidence: Int = 0,
            val solution: String = "", // <p>Remove all comments that return information that may help an attacker and fix any underlying problems they refer to.</p>
            val wascid: String = "" // 13
        ) {
            data class ZapInstance(
                val uri: String = "" // https://its-abq-showcase-uat-front-end-master.cz3.cloudapps.dhl.com
            )

            fun getSeverityString(): String {
                return when (riskcode) {
                    0 -> "NONE"
                    1 -> "LOW"
                    2 -> "MEDIUM"
                    3 -> "HIGH"
                    else -> "UNKNOWN"
                }
            }
        }

        private fun ZapAlert.toVulnerability() = Vulnerability(
            id = alertRef,
            severity = Severity.resilientValueOf(getSeverityString()),
            description = desc,
            origin = name,
            additionalInfo = mapOf(
                "wasc" to wascid,
                "cwe" to cweid,
                "confidence" to confidence,
                "locations" to instances.map { it.uri }
            )
        )

        fun getOpenVulnerabilityList(): List<Vulnerability> {
            return alerts.filter { it.confidence > 0 }.map { it.toVulnerability() }
        }

        fun getSuppressedVulnerabilityList(): List<Vulnerability> {
            return alerts.filter { it.confidence <= 0 }.map { it.toVulnerability() }
        }
    }
}

fun SecurityTestResult.Companion.from(zapTestResult: ZapTestResult, uri: String) =
    SecurityTestResult(
        uri = uri,
        date = zapTestResult.generated.atZone(ZoneId.systemDefault()),
        reportType = ReportType.DAST,
        tool = Tool(
            name = "OWASP Zed Attack Proxy",
            version = zapTestResult.version,
            ruleVersion = zapTestResult.version,
            vendor = "OWASP Zed Attack Proxy"
        ),
        vulnerabilities = Vulnerabilities(
            scannedObjects = zapTestResult.sites.size,
            vulnerableObjects = zapTestResult.sites.map { site ->
                site.alerts.map { alert -> alert.instances }.flatten()
            }.flatten().toSet().size,
            open = zapTestResult.sites.flatMap { site ->
                site.getOpenVulnerabilityList()
            },
            suppressed = zapTestResult.sites.flatMap { site ->
                site.getSuppressedVulnerabilityList()
            },
        )
    )
