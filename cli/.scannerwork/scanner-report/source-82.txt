package de.deutschepost.sdm.cdlib.mixins

import de.deutschepost.sdm.cdlib.change.metrics.model.Report
import de.deutschepost.sdm.cdlib.release.report.internal.*
import mu.KLogging
import picocli.CommandLine
import picocli.CommandLine.Option

class CheckMixin {

    @CommandLine.ArgGroup(validate = true, exclusive = false, heading = "", multiplicity = "0..1", order = 1)
    var webApprovalSection: WebApprovalSection = WebApprovalSection()

    class WebApprovalSection {
        @Option(
            names = ["--skip-cca"],
            description = ["Skips the vulnerability evaluation of the CCA report."],
            required = false
        )
        var skipCCA = false
    }


    private val reportSkipConfig by lazy { SecurityReportSkipConfig(skipCCA = webApprovalSection.skipCCA) }

    fun checkSecurityReports(
        reports: List<Report>,
        severity: Severity = Severity.HIGH
    ): SecurityReportVerificationResult =
        reports.map(Report::test).filterIsInstance<SecurityTestResult>().securityTestsVerify(severity, reportSkipConfig)

    fun checkOslcCompliance(appName: String, reports: List<Report>, isDistribution: Boolean) {
        val oslcTestResults = reports.map(Report::test).filterIsInstance<OslcTestResult>()

        if (oslcTestResults.isNotEmpty()) {
            logger.info { "Expected Policy Profile: ${if (isDistribution) OslcTestResult.PROFILE_DISTRIBUTION else OslcTestResult.PROFILE_NON_DISTRIBUTION}" }
        }
        oslcTestResults.oslcTestsVerify(appName, isDistribution)
    }

    companion object : KLogging()
}
