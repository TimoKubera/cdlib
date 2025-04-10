package de.deutschepost.sdm.cdlib.release


import de.deutschepost.sdm.cdlib.SubcommandWithHelp
import de.deutschepost.sdm.cdlib.mixins.CheckMixin
import de.deutschepost.sdm.cdlib.mixins.artifactory.ArtifactoryMixinLight
import de.deutschepost.sdm.cdlib.mixins.artifactory.UploadMixin
import de.deutschepost.sdm.cdlib.names.Names
import de.deutschepost.sdm.cdlib.release.mixin.ReportMixin
import de.deutschepost.sdm.cdlib.release.report.TestResultPrefixes
import de.deutschepost.sdm.cdlib.release.report.external.FnciTestResult
import de.deutschepost.sdm.cdlib.release.report.external.cca.CSS_QHCR_HARBOR
import de.deutschepost.sdm.cdlib.release.report.external.cca.HarborApiClient
import de.deutschepost.sdm.cdlib.release.report.external.cca.getCcaVulnerabilitiesUrl
import de.deutschepost.sdm.cdlib.release.report.external.fnci.FnciMessageWrapper
import de.deutschepost.sdm.cdlib.release.report.external.fnci.FnciServiceRepository
import de.deutschepost.sdm.cdlib.release.report.external.from
import de.deutschepost.sdm.cdlib.release.report.internal.SecurityTestResult
import de.deutschepost.sdm.cdlib.release.report.internal.Severity
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.OslcComplianceChecker
import de.deutschepost.sdm.cdlib.utils.defaultObjectMapper
import de.deutschepost.sdm.cdlib.utils.klogSelf
import de.deutschepost.sdm.cdlib.utils.resolveEnvByName
import io.micronaut.http.BasicAuth
import io.micronaut.http.HttpStatus
import jakarta.inject.Inject
import kotlinx.coroutines.*
import mu.KLogging
import picocli.CommandLine.*
import java.io.File
import java.util.concurrent.Callable
import kotlin.jvm.optionals.getOrNull
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@Command(
    name = "report",
    description = ["Lets you verify reports."],
    subcommands = [
        ReportCommand.CheckCommand::class,
        ReportCommand.FetchCommand::class,
        ReportCommand.UploadCommand::class,
    ]
)
class ReportCommand : SubcommandWithHelp() {

    @Command(
        name = "check",
        description = ["Checks your reports for known issues."],
        sortOptions = false
    )
    class CheckCommand : SubcommandWithHelp(), Callable<Int> {
        @Option(
            names = ["-s", "--severity"],
            description = ["Lowest Severity level that fails the check.", "One of: UNKNOWN, LOW, MEDIUM, HIGH, CRITICAL"],
        )
        var severity: Severity = Severity.HIGH

        @Option(
            names = ["--distribution"],
            description = ["Set OSLC checks to distribution or non-distribution policy."],
            required = false,
            negatable = true
        )
        var isDistribution = false

        @Option(
            names = ["--oslc-accepted-list"],
            required = false,
            description = [
                "Path to a JSON-File clarifying licenses for specific packages",
            ],
        )
        var oslcAcceptedListFile: File? = null

        @Mixin
        lateinit var checkMixin: CheckMixin

        @Mixin
        lateinit var reportMixin: ReportMixin

        override fun call(): Int {
            enableDebugIfOptionIsSet()
            return runCatching {
                var reports = reportMixin.reports
                if (reports.isEmpty()) { //TODO DEPRECATED: only for TQS soft remove reasons
                    return 0
                }
                if (checkMixin.checkSecurityReports(reports, severity).hasInvalidReport) return -1
                reports = OslcComplianceChecker.convertOslcPreResultsToOslcResults(
                    reports,
                    isDistribution,
                    oslcAcceptedListFile
                )
                checkMixin.checkOslcCompliance(resolveEnvByName(Names.CDLIB_APP_NAME), reports, isDistribution)
                0
            }.getOrElse {
                it.klogSelf(logger)
                -1
            }
        }
    }

    @Command(
        name = "upload",
        description = ["Analyzes and uploads your reports to artifactory."]
    )
    class UploadCommand : SubcommandWithHelp(), Callable<Int> {
        @Option(
            names = ["--distribution"],
            description = ["Set OSLC checks to distribution or non-distribution policy."],
            required = false,
            negatable = true
        )
        var isDistribution: Boolean = false

        @Option(
            names = ["--oslc-accepted-list"],
            required = false,
            description = [
                "Path to a JSON-File clarifying licenses for specific packages",
            ],
        )
        var oslcAcceptedListFile: File? = null

        @Mixin
        lateinit var checkMixin: CheckMixin

        @Mixin
        lateinit var uploadMixin: UploadMixin

        @Mixin
        lateinit var reportMixin: ReportMixin

        @Suppress("UNUSED")
        @Mixin
        lateinit var artifactoryMixinLight: ArtifactoryMixinLight

        override fun call(): Int {
            enableDebugIfOptionIsSet()

            return runCatching {
                var reports = reportMixin.reports
                if (reports.isEmpty()) { //TODO DEPRECATED: only for TQS soft remove reasons
                    return 0
                }
                if (checkMixin.checkSecurityReports(reports).hasInvalidReport) return -1
                reports = OslcComplianceChecker.convertOslcPreResultsToOslcResults(
                    reports,
                    isDistribution,
                    oslcAcceptedListFile
                )
                checkMixin.checkOslcCompliance(resolveEnvByName(Names.CDLIB_APP_NAME), reports, isDistribution)
                uploadMixin.upload(reports)
                uploadMixin.upload(reportMixin.files)
                0
            }.getOrElse {
                it.klogSelf(logger)
                -1
            }
        }
    }


    //cdlib report fetch css --image bla:blub --robot-account css_qhcr_robot_foo --token secret
    //cdlib report fetch fnci --fnci-token CodeInsightToken --project-id 0228

    @Command(
        name = "fetch",
        description = ["Fetches your reports from remote scanners like CSS or Revenera Code Insight."],
        subcommands = [FetchCommand.FnciCommand::class, FetchCommand.CSSCommand::class]
    )
    class FetchCommand : SubcommandWithHelp() {
        @Command(
            name = "css",
            description = ["Fetches your CCA reports from QHCR Harbor."],
            sortOptions = false
        )
        class CSSCommand : SubcommandWithHelp(), Callable<Int> {
            @Option(
                names = ["-i", "--image"],
                required = true,
                description = [
                    "URL of the image e.g. dpdhl.css-qhcr-pi.azure.deutschepost.de/cdlib/cdlib-cli:20220609.1146.13-master",
                ],
            )
            lateinit var image: String

            @Option(
                names = ["-r", "--robot-account"],
                required = true,
                description = ["Name of the robot account"],
            )
            lateinit var robotAccount: String

            @Option(
                names = ["-t", "--token"],
                required = true,
                description = ["Token of the robot account for authentication"],
            )
            lateinit var token: String

            @Option(
                names = ["--timeout-in-minutes"],
                required = false,
                description = ["Timeout for waiting on css scan in minutes. Default is 10 minutes."],
            )
            var cssTimeout: Int = 10

            @Inject
            lateinit var harborClient: HarborApiClient
            override fun call(): Int = runBlocking {
                enableDebugIfOptionIsSet()
            
                val (registry, project, repositoryReference) = try {
                    parseImage(image)
                } catch (e: Exception) {
                    logger.error { "Failed to parse image string $image: ${e.message}" }
                    return@runBlocking -1
                }
            
                if (!registry.contains("css", ignoreCase = true) || !registry.contains("deutschepost.de", ignoreCase = true)) {
                    logger.error { "Invalid container registry $registry" }
                    logger.error { "Only $CSS_QHCR_HARBOR is supported at the moment." }
                    return@runBlocking -1
                }
            
                val (repository, reference) = try {
                    parseRepositoryReference(repositoryReference)
                } catch (e: Exception) {
                    logger.error { "Failed to parse $repositoryReference: ${e.message}" }
                    return@runBlocking -1
                }
            
                val releaseName = resolveEnvByName(Names.CDLIB_RELEASE_NAME)
            
                val auth = BasicAuth(robotAccount, token)
            
                logger.info {
                    "Polling image $registry/$project/$repository:$reference"
                }
            
                harborClient.checkIfPresent(registry, project, repository, reference, auth).let { response ->
                    val body = response.body.get()
                    if (body == "[]\n") {
                        logger.error { "Failed to find image in Harbor." }
                        return@runBlocking -1
                    }
                    logger.info { "Image is present in Harbor." }
                }
            
                val pollResult = pollScanStatus(registry, project, repository, reference, auth, cssTimeout)
                if (!pollResult) {
                    logger.error { "Scan did not complete within $cssTimeout minutes. Terminating now..." }
                    return@runBlocking -1
                } else {
                    logger.info { "Harbor CCA scan completed" }
                }
            
                val ccaVulnerabilitiesUrl = getCcaVulnerabilitiesUrl(registry, project, repository, reference)
                logger.info { "Fetching CCA report: $ccaVulnerabilitiesUrl" }
            
                val externalCcaReport = runCatching {
                    harborClient.getVulnerabilities(
                        registry = registry,
                        project = project,
                        repository = repository,
                        reference = reference,
                        basicAuth = auth
                    )
                }.getOrElse {
                    logger.error { "Failed fetching CCA report: ${it.message}" }
                    return@runBlocking -1
                }.let {
                    logger.debug { "status: ${it.status}\nbody: ${it.body()}" }
                    val body = it.body()
                    if (it.status != HttpStatus.OK || body == null) {
                        logger.error { "Failed fetching CCA report.\nstatus: ${it.status}\nbody:${it.body}" }
                        return@runBlocking -1
                    }
                    body
                }
            
                logger.info { "Getting $project CVE allow list" }
                val ccaSuppressionList = runCatching {
                    harborClient.getCveAllowList(registry, project, auth)
                }.getOrElse {
                    logger.error { "Failed fetching CVE allow list: ${it.message}" }
                    return@runBlocking -1
                }.let {
                    logger.debug { "status: ${it.status}\nbody: ${it.body()}" }
                    val body = it.body()
                    if (it.status != HttpStatus.OK || body == null) {
                        logger.error { "Failed fetching CVE allow list.\nstatus: ${it.status}\nbody:${it.body}" }
                        return@runBlocking -1
                    }
                    body
                }
            
                val ccaReport = SecurityTestResult.from(externalCcaReport, ccaSuppressionList, ccaVulnerabilitiesUrl)
                logger.debug { "Created Trivy CCA report:\n $ccaReport" }
            
                val fileName = "${TestResultPrefixes.DEFAULT_PREFIX_CCA}-trivy-$releaseName.json"
                logger.info { "Writing Trivy CCA report to $fileName" }
                ccaReport.writeJson(fileName)
            
                return@runBlocking 0
            }
            
            private fun parseImage(image: String): Triple<String, String, String> {
                val parts = image.split("/", limit = 3)
                require(parts.size == 3) { "Image string must contain exactly 3 parts" }
                return Triple(parts[0], parts[1], parts[2])
            }
            
            private fun parseRepositoryReference(repositoryReference: String): Pair<String, String> {
                val parts = repositoryReference.replace("/", "%2F").split(":", limit = 2)
                require(parts.size == 2) { "Repository reference must contain exactly 2 parts" }
                return Pair(parts[0], parts[1])
            }
            
            private suspend fun pollScanStatus(
                registry: String,
                project: String,
                repository: String,
                reference: String,
                auth: BasicAuth,
                timeout: Int
            ): Boolean {
                return withTimeoutOrNull(timeout.toDuration(DurationUnit.MINUTES)) {
                    while (true) {
                        val response = runCatching {
                            harborClient.getScanStatus(
                                registry = registry,
                                project = project,
                                repository = repository,
                                reference = reference,
                                basicAuth = auth
                            )
                        }.getOrElse {
                            logger.error { "Failed to get scanStatus: ${it.message}" }
                            return@withTimeoutOrNull false
                        }
                        val body = response.body()
                        logger.debug { "status: ${response.status}\nbody: $body" }
            
                        when {
                            response.status == HttpStatus.OK && body?.isStatusSuccess() == true -> return@withTimeoutOrNull true
                            response.status == HttpStatus.OK && body?.type == null -> {
                                logger.error { "Failed to find image in Harbor." }
                                return@withTimeoutOrNull false
                            }
                            response.status == HttpStatus.OK && body?.type != "IMAGE" -> {
                                logger.error { "Only images are supported! You tried to scan a ${body?.type} m(" }
                                return@withTimeoutOrNull false
                            }
                            else -> {
                                logger.info { "Checking scan status in 5 seconds again" }
                                delay(5.toDuration(DurationUnit.SECONDS))
                            }
                        }
                    }
                } ?: false
            }
        }

        @Command(
            name = "fnci",
            description = ["Fetches your OSLC reports from Revenera FNCI."],
            sortOptions = false
        )
        class FnciCommand : SubcommandWithHelp(), Callable<Int> {
            @Option(
                names = ["--fnci-token"],
                required = true,
                description = ["Token of the robot account for authentication"],
            )
            lateinit var token: String

            @Option(
                names = ["--project-id"],
                required = true,
                description = ["ProjectId of your FNCI Project"],
            )
            var projectId: Int = 0

            @Option(
                names = ["--timeout-in-minutes"],
                required = false,
                description = ["Timeout for waiting on fnci scan in minutes. Default is 10 minutes."],
            )
            var fnciTimeout: Int = 10

            @Inject
            lateinit var fnciService: FnciServiceRepository

            override fun call(): Int = runBlocking(Dispatchers.IO) {
                runCatching {
                    withTimeoutOrNull(fnciTimeout.toDuration(DurationUnit.MINUTES)) {
                        enableDebugIfOptionIsSet()
                        val releaseName = resolveEnvByName(Names.CDLIB_RELEASE_NAME)

                        logger.info { "Starting generating a report" }
                        val taskId = fnciService.generateReport(projectId, 1, token)
                        logger.info { "Successfully generated taskID: $taskId" }

                        val projectInfoJob = async { fnciService.getProjectInformation(projectId, token) }
                        val inventoryJob = async { fnciService.getProjectInventory(projectId, token) }
                        val reportJob = async { fetchReport(taskId) }

                        val projectInfo = projectInfoJob.await()
                        val inventory = inventoryJob.await()

                        defaultObjectMapper.writeValue(
                            File("${TestResultPrefixes.DEFAULT_PREFIX_FNCI}-$releaseName.json"),
                            FnciTestResult(projectInfo, inventory)
                        )

                        reportJob.await().let {
                            when (it) {
                                is ByteArray -> {
                                    val filename = "${TestResultPrefixes.DEFAULT_PREFIX_FNCI}-$releaseName.zip"
                                    logger.info { "Successfully fetched FNCI report to $filename" }
                                    File(filename).writeBytes(it)
                                }
                                else -> {
                                    logger.error { "Report could not be fetched" }
                                    return@withTimeoutOrNull -1
                                }
                            }
                        }.let {
                            when (it) {
                                null -> {
                                    logger.error { "Scan did not complete within $fnciTimeout minutes. Terminating now..." }
                                }
                                -1 -> return@runBlocking -1
                            }
                        }

                        return@runBlocking 0
                    }.getOrElse {
                        it.klogSelf(logger)
                        -1
                    }

                    private suspend fun fetchReport(taskId: Int): Any? {
                        logger.info { "Client will try to fetch report" }
                        while (true) {
                            val response = fnciService.downloadReport(projectId, 1, taskId, token)

                            when (response.status) {
                                HttpStatus.ACCEPTED -> {
                                    val message = response.body.getOrNull()?.let {
                                        runCatching {
                                            defaultObjectMapper.readValue(it, FnciMessageWrapper::class.java).data.firstOrNull()?.message
                                        }.getOrNull()
                                    } ?: "Report generation is still in progress."
                                    logger.info { "$message Client will retry after 20 seconds" }
                                    delay(20_000)
                                }

                                HttpStatus.OK -> {
                                    return response.body.get()
                                }

                                else -> {
                                    return null
                                }
                            }
                        }
                    }
    }

    companion object : KLogging()
}
