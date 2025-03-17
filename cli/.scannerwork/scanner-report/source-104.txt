package de.deutschepost.sdm.cdlib.release.report.external.cca


import com.fasterxml.jackson.annotation.JsonProperty

data class ScanStatus(
    @JsonProperty("scan_overview")
    val scanOverview: ScanOverview?,
    val type: String
) {
    data class ScanOverview(
        @JsonProperty("application/vnd.security.vulnerability.report; version=1.1")
        val applicationvndSecurityVulnerabilityReportVersion11: ApplicationvndSecurityVulnerabilityReportVersion11
    ) {
        data class ApplicationvndSecurityVulnerabilityReportVersion11(
            @JsonProperty("scan_status")
            val scanStatus: String,
        )
    }

    fun isStatusSuccess(): Boolean =
        scanOverview?.applicationvndSecurityVulnerabilityReportVersion11?.scanStatus == "Success"
}
