package de.deutschepost.sdm.cdlib.release.report.internal

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import mu.KLogging
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class SecurityTestResult(
    override val uri: String,
    override val date: ZonedDateTime,
    @JsonAlias("testResultType")
    override val reportType: ReportType,
    override val tool: Tool,
    val vulnerabilities: Vulnerabilities,
    val pregenerated: Boolean = false,
) : TestResult() {

    @JsonIgnore
    fun isValid(reportSkipConfig: SecurityReportSkipConfig): Boolean {
        if (reportSkipConfig.isSkipable(this)) {
            return true
        }
        if (pregenerated) {
            return checksForPregeneratedReports()
        }
        if (reportType == ReportType.SCA) {
            if (LocalDateTime.parse(tool.ruleVersion, DateTimeFormatter.ISO_DATE_TIME)
                    .atZone(ZoneId.systemDefault()) < ZonedDateTime.now().minusMonths(1)
            ) {
                logger.error { "[${reportType}] Generated using NVD CVE Modified older than one month: (${tool.ruleVersion})" }
                return false
            } else {
                logger.info { "[${reportType}] Current NVD CVE Modified used: ${tool.ruleVersion}." }
            }
        }
        logger.info { info }
        if (date < ZonedDateTime.now().minusMonths(1)) {
            logger.error { "[${reportType}] Outdated report. (${date})" }
            return false
        } else {
            logger.info { "[${reportType}] Report up to date. ${date}." }
        }

        logger.info { "[${reportType}] Generated using tool version: ${tool.version}." }
        return true
    }

    private fun checksForPregeneratedReports(): Boolean {
        val errors = mutableListOf<String>()
        errors.addAll(getSeverityCountErrors())
        if (date > ZonedDateTime.now().plusDays(1)) {
            errors.add("Report can't be from the future! ($date)")
        }
        if (vulnerabilities.suppressed.count() != vulnerabilities.suppressedCount) {
            errors.add("Wrong number of suppressions: ${vulnerabilities.suppressed.count()} counted vs ${vulnerabilities.suppressedCount} recorded.")
        }
        errors.forEach {
            logger.error { it }
        }
        return errors.isEmpty()
    }

    private fun getSeverityCountErrors(): List<String> {
        val sevCounts = vulnerabilities.severityCounts.asMap()
        return sequence {
            Severity.values().map { severity ->
                val count = vulnerabilities.open.count { it.severity == severity }
                if (count != sevCounts[severity]) {
                    yield("Wrong number of vulnerabilities for $severity: $count counted vs ${sevCounts[severity]} recorded.")
                }
            }
        }.toList()
    }

    fun getRelevantVulnerabilities(
        threshold: Severity,
        reportSkipConfig: SecurityReportSkipConfig
    ): List<Vulnerability> {
        fun Vulnerability.logString() = "[$reportType] ${this.severity} - ${this.id} in ${this.origin}"
        logger.info { info }
        with(vulnerabilities) {
            listOf(
                "Vulnerability Stats:",
                "  CRITICAL:    ${severityCounts.critical}",
                "  HIGH:        ${severityCounts.high}",
                "  MEDIUM:      ${severityCounts.medium}",
                "  LOW:         ${severityCounts.low}",
                "  NONE/INFO:   ${severityCounts.none}",
                "  UNKNOWN:     ${severityCounts.unknown}",
                "  Open:        $openCount",
                "  Suppressed:  $suppressedCount"
            ).forEach { logger.info { "[$reportType] $it" } }

            if (logger.underlyingLogger.isDebugEnabled) {
                open.filter { vulnerability ->
                    vulnerability.severity < threshold
                }.onEach { logger.debug { it.toString() } }

                suppressed.forEach {
                    if (it is Vulnerability) {
                        logger.debug { it.logString() }
                    } else {
                        logger.info { "[$reportType] $it" }
                    }
                }
            }

            if (reportSkipConfig.isSkipable(this@SecurityTestResult)) {
                logger.warn { "Skipping build breaker for [$reportType]." }
                return emptyList()
            }

            val vulnList = open.filter { vulnerability ->
                vulnerability.severity >= threshold
            }.onEach {
                logger.error { it.logString() }
            }
            if (vulnList.isNotEmpty()) {
                logger.error {
                    """ [$reportType] has relevant vulnerabilities at
                            $uri

                            To fix these errors investigate the report and handle the vulnerabilities according to CDlib tutorials.
                    """.trimIndent()
                }
            }
            return vulnList
        }
    }

    companion object : KLogging()
}

fun List<SecurityTestResult>.securityTestsVerify(
    failureSeverity: Severity,
    reportSkipConfig: SecurityReportSkipConfig
): SecurityReportVerificationResult {
    val securityReportVerificationResult = SecurityReportVerificationResult()
    forEach { testResult ->
        if (testResult.getRelevantVulnerabilities(failureSeverity, reportSkipConfig).isNotEmpty() or
            !testResult.isValid(reportSkipConfig)
        ) {
            securityReportVerificationResult.hasInvalidReport = true
        }
        when (testResult.reportType) {
            ReportType.DAST -> securityReportVerificationResult.hasDAST = true
            ReportType.SCA -> securityReportVerificationResult.hasSCA = true
            ReportType.SAST -> securityReportVerificationResult.hasSAST = true
            // ReportType.CCA is currently unhandled. This is a placeholder for potential future logic.
            ReportType.OTHER -> {}
            ReportType.OSLC -> {}
            ReportType.OSLC_PRE -> {}
        }
    }
    return securityReportVerificationResult
}

fun List<TestResult>.securityTestsSuppressions(): Map<String, Map<String, List<Any>>> {
    return ReportType.values().associate { type ->
        type.toString() to
            filterIsInstance<SecurityTestResult>()
                .filter { report ->
                    report.reportType == type
                }.associate { report ->
                    report.uri to report.vulnerabilities.suppressed
                }
    }
}

data class SecurityReportVerificationResult(
    var hasInvalidReport: Boolean = false,
    var hasSAST: Boolean = false,
    var hasDAST: Boolean = false,
    var hasSCA: Boolean = false,
)

class SecurityReportSkipConfig(val skipCCA: Boolean = false) {
    val skipSAST = false
    val skipDAST = false
    val skipGeneric = false
    val skipSCA = false
    val skipOSLC = false

    fun isSkipable(testResult: SecurityTestResult) =
        when (testResult.reportType) {
            ReportType.OTHER -> skipGeneric
            ReportType.CCA -> skipCCA
            ReportType.SAST -> skipSAST
            ReportType.SCA -> skipSCA
            ReportType.DAST -> skipDAST
            ReportType.OSLC -> skipOSLC
            ReportType.OSLC_PRE -> skipOSLC
        }
}
