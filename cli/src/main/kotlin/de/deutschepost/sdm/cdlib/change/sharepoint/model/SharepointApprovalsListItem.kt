package de.deutschepost.sdm.cdlib.change.sharepoint.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import de.deutschepost.sdm.cdlib.change.metrics.model.Report
import de.deutschepost.sdm.cdlib.release.report.internal.ReportType
import de.deutschepost.sdm.cdlib.release.report.internal.SecurityTestResult
import mu.KLogging

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SharepointApprovalsListItem(
    @JsonProperty("__metadata")
    val metadata: Metadata = Metadata.PROD,
    @JsonProperty("report_x002d_url")
    val reportUrl: String,
    @JsonProperty("identifier")
    val identifier: String,
    @JsonProperty("Pipeline_x0020_Approval_x0020_CoId")
    val applicationId: Int,
    @JsonProperty("job_x002d_url")
    val jobUrl: String,


    @JsonProperty("Open_x0020_DAST_x0020_Findings_x")
    val dastCritical: Int = 0,
    @JsonProperty("Open_x0020_DAST_x0020_Findings_x0")
    val dastHigh: Int = 0,
    @JsonProperty("Open_x0020_DAST_x0020_Findings_x1")
    val dastMedium: Int = 0,
    @JsonProperty("Open_x0020_DAST_x0020_Findings_x2")
    val dastLow: Int = 0,

    @JsonProperty("Open_x0020_SAST_x0020_Findings_x")
    val sastCritical: Int = 0,
    @JsonProperty("Open_x0020_SAST_x0020_Findings_x0")
    val sastHigh: Int = 0,
    @JsonProperty("Open_x0020_SAST_x0020_Findings_x1")
    val sastMedium: Int = 0,
    @JsonProperty("Open_x0020_SAST_x0020_Findings_x2")
    val sastLow: Int = 0,

    @JsonProperty("Open_x0020_SCA_x0020_Findings_x0")
    val scaCritical: Int = 0,
    @JsonProperty("Open_x0020_SCA_x0020_Findings_x00")
    val scaHigh: Int = 0,
    @JsonProperty("Open_x0020_SCA_x0020_Findings_x01")
    val scaMedium: Int = 0,
    @JsonProperty("Open_x0020_SCA_x0020_Findings_x02")
    val scaLow: Int = 0,


    @JsonProperty("SAST_x0020_performed_x003f_")
    val sastPerformed: Boolean = false,
    @JsonProperty("SCA_x0020_performed_x003f_")
    val scaPerformed: Boolean = false,
    @JsonProperty("DAST_x0020_performed_x003f_")
    val dastPerformed: Boolean = false,

    @JsonProperty("Suppressed_x0020_DAST_x0020_Find")
    val dastSuppressed: Int = 0,
    @JsonProperty("Suppressed_x0020_SAST_x0020_Find")
    val sastSuppressed: Int = 0,
    @JsonProperty("Suppressed_x0020_SCA_x0020_Findi")
    val scaSupressed: Int = 0,

    ) : SharepointListItem {
    data class Metadata(
        @JsonProperty("type")
        val type: String
    ) {
        companion object {
            val PROD = Metadata("SP.Data.Pipeline_x0020_ApprovalsListItem")
            val TEST = Metadata("SP.Data.Pipeline_x0020_Approvals_x0020_TESTListItem")
        }
    }

    @JsonIgnore
    override val listNameProd = "Pipeline%20Approvals"

    @JsonIgnore
    override val listNameTest = "Pipeline%20Approvals%20TEST"

    fun addReports(reports: List<Report>): SharepointApprovalsListItem =
        reports.map(Report::test).filterIsInstance<SecurityTestResult>()
            .fold(this, SharepointApprovalsListItem::addTestResult)


    private fun addTestResult(testResult: SecurityTestResult): SharepointApprovalsListItem {
        val vulnerabilities = testResult.vulnerabilities
        val counts = vulnerabilities.severityCounts

        return when (testResult.reportType) {
            ReportType.SAST ->
                copy(
                    sastPerformed = true,
                    sastCritical = sastCritical + counts.critical,
                    sastHigh = sastHigh + counts.high,
                    sastMedium = sastMedium + counts.medium,
                    sastLow = sastLow + counts.low,
                    sastSuppressed = sastSuppressed + vulnerabilities.suppressedCount
                )

            ReportType.CCA -> this

            ReportType.SCA ->
                copy(
                    scaPerformed = true,
                    scaCritical = scaCritical + counts.critical,
                    scaHigh = scaHigh + counts.high,
                    scaMedium = scaMedium + counts.medium,
                    scaLow = scaLow + counts.low,
                    scaSupressed = scaSupressed + vulnerabilities.suppressedCount
                )

            ReportType.DAST ->
                copy(
                    dastPerformed = true,
                    dastCritical = dastCritical + counts.critical,
                    dastHigh = dastHigh + counts.high,
                    dastMedium = dastMedium + counts.medium,
                    dastLow = dastLow + counts.low,
                    dastSuppressed = dastSuppressed + vulnerabilities.suppressedCount
                )

            ReportType.OTHER -> this

            ReportType.OSLC -> this

            ReportType.OSLC_PRE -> this
        }
    }

    companion object : KLogging()
}
