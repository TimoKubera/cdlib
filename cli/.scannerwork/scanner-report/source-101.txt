package de.deutschepost.sdm.cdlib.release.mixin

import de.deutschepost.sdm.cdlib.change.metrics.model.Report
import de.deutschepost.sdm.cdlib.mixins.FilesMixin
import de.deutschepost.sdm.cdlib.release.report.TestResultParser
import de.deutschepost.sdm.cdlib.release.report.TestResultPrefixes
import de.deutschepost.sdm.cdlib.utils.sha256sum
import mu.KLogging
import picocli.CommandLine.Mixin
import picocli.CommandLine.Option
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.name

class ReportMixin {
    @Mixin
    lateinit var filesMixin: FilesMixin

    @Option(
        names = ["-g", "--generic"],
        description = ["Parses this file as a generic report.", "Can be specified multiple times.", "Does NOT support ANT patterns!"],
        required = false
    )
    var genericReports: List<String> = emptyList()

    @Option(
        names = ["--report-prefix-zap", "--report-prefix-dast"],
        description = ["Prefix for ZAP's files. Default: '\${DEFAULT-VALUE}'."],
        defaultValue = TestResultPrefixes.DEFAULT_PREFIX_ZAP,
    )
    lateinit var zap: String

    @Option(
        names = ["--report-prefix-odc", "--report-prefix-sca"],
        description = ["Prefix for Dependency Check's files. Default: '\${DEFAULT-VALUE}'."],
        defaultValue = TestResultPrefixes.DEFAULT_PREFIX_ODC,
    )
    lateinit var odc: String

    @Option(
        names = ["--report-prefix-fortify", "--report-prefix-sast"],
        description = ["Prefix for Fortify's files. Default: '\${DEFAULT-VALUE}'."],
        defaultValue = TestResultPrefixes.DEFAULT_PREFIX_FORTIFY,
    )
    lateinit var fortify: String

    @Option(
        names = ["--report-prefix-trivy", "--report-prefix-cca"],
        description = ["Prefix for Trivy's CCA files. Default: '\${DEFAULT-VALUE}'."],
        defaultValue = TestResultPrefixes.DEFAULT_PREFIX_CCA,
    )
    lateinit var cca: String

    @Deprecated("Deprecated due to unsupported plugin and measurements. This flag has no effect and can be removed.")
    @Option(
        names = ["--report-prefix-tqs"],
        description = ["Prefix for TQS reports. Default: '\${DEFAULT-VALUE}'."],
        defaultValue = "TQS_Reports"
    )
    lateinit var tqs: String

    @Option(
        names = ["--report-prefix-oslc-maven-plugin"],
        description = ["Prefix for OSLC reports. Default: '\${DEFAULT-VALUE}'."],
        defaultValue = TestResultPrefixes.DEFAULT_PREFIX_OSLC_MAVEN_PLUGIN,
    )
    lateinit var oslcMavenPlugin: String

    @Option(
        names = ["--report-prefix-oslc-gradle-plugin"],
        description = ["Prefix for OSLC reports. Default: '\${DEFAULT-VALUE}'."],
        defaultValue = TestResultPrefixes.DEFAULT_PREFIX_OSLC_GRADLE_PLUGIN,
    )
    lateinit var oslcGradlePlugin: String

    @Option(
        names = ["--report-prefix-oslc-npm-plugin"],
        description = ["Prefix for OSLC reports. Default: '\${DEFAULT-VALUE}'."],
        defaultValue = TestResultPrefixes.DEFAULT_PREFIX_OSLC_NPM_PLUGIN,
    )
    lateinit var oslcNPMPlugin: String

    @Option(
        names = ["--report-prefix-oslc"],
        description = ["Prefix for OSLC reports. Default: '\${DEFAULT-VALUE}'."],
        defaultValue = TestResultPrefixes.DEFAULT_PREFIX_FNCI,
    )
    lateinit var oslcFNCI: String

    val files by lazy { (filesMixin.getFiles(tqs) + genericFiles()) }

    val reports: List<Report> by lazy {
        val reports = parseReports(files, substitutes = genericBaseNames())
        check(reports.isNotEmpty() || filesMixin.hasTqs) {
            "Found no reports!"
        }
        reports
    }

    private fun genericBaseNames(): List<String> {
        return genericReports.map { Path(it).name }
    }

    private fun genericFiles(): List<File> {
        return genericReports.map {
            File(it).also { file ->
                require(file.exists()) {
                    "File ${file.absolutePath} does not exist."
                }
                logger.debug { "Found file: ${file.absolutePath}" }
            }
        }
    }

    private fun parseReports(files: List<File>, substitutes: List<String>): List<Report> =
        files.mapNotNull { file: File ->
            TestResultParser.parse(file, testResultPrefixes(), substitutes)?.let { testResult ->
                Report(test = testResult, testHash = file.sha256sum())
            }
        }

    private fun testResultPrefixes() =
        TestResultPrefixes(zap, odc, fortify, cca, oslcFNCI, oslcMavenPlugin, oslcGradlePlugin, oslcNPMPlugin)

    companion object : KLogging()
}
