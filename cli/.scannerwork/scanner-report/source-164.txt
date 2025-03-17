package de.deutschepost.sdm.cdlib.change.sharepoint

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import de.deutschepost.sdm.cdlib.change.metrics.model.Report
import de.deutschepost.sdm.cdlib.change.sharepoint.model.SharepointApprovalsListItem
import de.deutschepost.sdm.cdlib.release.report.internal.ReportType
import de.deutschepost.sdm.cdlib.release.report.internal.SecurityTestResult
import de.deutschepost.sdm.cdlib.release.report.internal.Tool
import de.deutschepost.sdm.cdlib.release.report.internal.Vulnerabilities
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.SystemEnvironmentTestListener
import io.kotest.matchers.nulls.shouldNotBeNull
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

@RequiresTag("IntegrationTest")
@Tags("IntegrationTest")
@MicronautTest
class SharepointClientIntegrationTest(
    @Value("\${sharepoint.username}") val username: String,
    @Value("\${sharepoint.password}") val password: String,
) : StringSpec() {
    private val sharepointClient = SharepointClient(username, password)
    private val sast by lazy { generateReport(ReportType.SAST, 11, 12, 13, 14, 15) }
    private val sca by lazy { generateReport(ReportType.SCA, 21, 22, 23, 24, 25) }
    private val dast by lazy { generateReport(ReportType.DAST, 31, 32, 33, 34, 35) }
    private val reports by lazy { listOf(sast, sca, dast) }

    override fun listeners(): List<TestListener> = listOf(
        SystemEnvironmentTestListener(
            mapOf(
                "CDLIB_APP_NAME" to "cli",
                "CDLIB_PM_GIT_MAIL" to "integration-test-git-mail",
                "CDLIB_PM_GIT_NAME" to "integration-test-git-author",
                "CDLIB_PM_GIT_ID" to "integration-test-git-id",
                "CDLIB_PM_GIT_LINK" to "integration-test-git-link",
                "CDLIB_PM_GIT_MESSAGE" to "integration-test-git-message",
                "CDLIB_PM_GIT_ORIGIN" to "integration-test-git-origin",
                "CDLIB_CICD_PLATFORM" to "integration-test-platform",
                "CDLIB_JOB_URL" to "https://integration-test-url.jenkuns.example.com/foo/bar/job/1337",
            ), OverrideMode.SetOrOverride
        )
    )

    init {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        loggerContext.getLogger("de.deutschepost.sdm").level = Level.DEBUG



        "Add test values" {
            val entry = SharepointApprovalsListItem(
                jobUrl = "test-job-url",
                identifier = "test-release-name",
                reportUrl = "test-report-url",
                applicationId = 5
            ).addReports(reports)

            shouldNotThrowAny {
                val ret = sharepointClient.addEntryTest(entry)
                ret.shouldNotBeNull()
            }
        }

        "Add test values two times" {
            val entry = SharepointApprovalsListItem(
                jobUrl = "test-job-url",
                identifier = "test-release-name",
                reportUrl = "test-report-url",
                applicationId = 5
            ).addReports(reports).addReports(reports)


            shouldNotThrowAny {
                val ret = sharepointClient.addEntryTest(entry)
                ret.shouldNotBeNull()
            }
        }
    }

    private fun generateReport(
        reportType: ReportType,
        critical: Int,
        high: Int,
        medium: Int,
        low: Int,
        suppressed: Int
    ): Report {
        val tool = Tool("name", "1337", "vendor")
        val result = SecurityTestResult(
            "uri",
            ZonedDateTime.now(),
            reportType,
            tool = tool,
            Vulnerabilities(
                scannedObjects = 0,
                vulnerableObjects = 0,
                severityCounts = Vulnerabilities.SeverityCounts(
                    critical = critical,
                    high = high,
                    medium = medium,
                    low = low,
                    none = 0,
                    unknown = 0
                ),
                suppressedCount = suppressed
            )
        )
        return Report(test = result, testHash = "irrelevant")
    }
}
