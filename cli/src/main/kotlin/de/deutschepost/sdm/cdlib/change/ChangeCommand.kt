package de.deutschepost.sdm.cdlib.change

import de.deutschepost.sdm.cdlib.SubcommandWithHelp
import de.deutschepost.sdm.cdlib.artifactory.ArtifactoryClient
import de.deutschepost.sdm.cdlib.artifactory.ArtifactoryInstanceSection
import de.deutschepost.sdm.cdlib.change.changemanagement.api.ChangeHandler
import de.deutschepost.sdm.cdlib.change.changemanagement.api.ChangeManagementRepository
import de.deutschepost.sdm.cdlib.change.changemanagement.model.*
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.ChangePhaseId.*
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.Labels
import de.deutschepost.sdm.cdlib.change.metrics.client.CosmosDashboardRepository
import de.deutschepost.sdm.cdlib.change.metrics.model.*
import de.deutschepost.sdm.cdlib.mixins.CheckMixin
import de.deutschepost.sdm.cdlib.mixins.artifactory.ArtifactoryMixinFull
import de.deutschepost.sdm.cdlib.mixins.sharepoint.OslcMixin
import de.deutschepost.sdm.cdlib.mixins.sharepoint.SharepointClientMixin
import de.deutschepost.sdm.cdlib.mixins.sharepoint.WebapprovalMixin
import de.deutschepost.sdm.cdlib.names.Names
import de.deutschepost.sdm.cdlib.release.report.internal.ReportType
import de.deutschepost.sdm.cdlib.release.report.internal.Tool
import de.deutschepost.sdm.cdlib.release.report.internal.securityTestsSuppressions
import de.deutschepost.sdm.cdlib.utils.defaultObjectMapper
import de.deutschepost.sdm.cdlib.utils.klogSelf
import de.deutschepost.sdm.cdlib.utils.resolveEnvByName
import jakarta.inject.Inject
import mu.KLogging
import picocli.CommandLine.*
import picocli.CommandLine.Model.CommandSpec
import java.io.File
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.Callable
import kotlin.time.toKotlinDuration


@Command(
    name = "change",
    description = ["Contains subcommands for the Change part of your Pipeline."],
    subcommands = [ChangeCommand.CloseCommand::class, ChangeCommand.CreateCommand::class]
)
class ChangeCommand : SubcommandWithHelp() {
    @Command(
        name = "create",
        description = ["Creates change request with Change Managements Jira API."],
        defaultValueProvider = CreateCommand.ChangeDetails::class,
        requiredOptionMarker = '*'
    )
    class CreateCommand : SubcommandWithHelp(), Callable<Int> {

        /*
        * Note about ordering of ArgGroups: When options belonging together are spread across mixins and subcommands, they can be placed behind the corresponding group in the parent command via ordering (they still need an empty heading).
        * So the ArgGroup needs and order with an even number, e.g. 6, and any group that should be placed under this group gets the order one higher, e.g. 7. That way all options appear together under the same heading.
         */

        val auth: String by lazy { "Bearer ${changeManagementSection.jiraToken}" }

        @Suppress("UNUSED")
        @Mixin
        lateinit var sharepointClientMixin: SharepointClientMixin

        @ArgGroup(
            validate = true,
            exclusive = false,
            heading = "Options for Change Management (Jira):%n",
            multiplicity = "1",
            order = 0
        )
        lateinit var changeManagementSection: ChangeManagementSection

        class ChangeManagementSection {
            @Option(
                names = ["--token", "--jira-token"],
                description = ["Personal Access Token (Bearer) for the Change Management Jira API."],
                required = true
            )
            lateinit var jiraToken: String

            @Option(
                names = ["--commercial-reference"],
                description = ["Number of the commercial reference (i.e. Leistungsschein) to use with the change management API."],
                required = true
            )
            lateinit var commercialReference: String

            @Option(
                names = ["--resume"],
                description = ["Instead of closing all open changes for this pipeline from previous runs, this will resume (start and close) the latest open change."],
                required = false,
                arity = "1"
            )
            var resume = false

            @Option(
                names = ["--comment"],
                description = ["Adds the given comment to the Jira issue. "],
                required = false,
                defaultValue = ""
            )
            lateinit var comment: String

            @Option(
                names = ["--enforce-frozen-zone"],
                description = ["Enforces the frozen zone for testing environment."],
                required = false,
                hidden = true
            )
            var enforceFrozenZone = false

            @Option(
                names = ["--skip-approval-wait"],
                description = ["Skips the wait for change approval."],
                required = false,
                hidden = true
            )
            var skipApprovalWait = false

            // create change --distribution/--no-distribution
            // create change --no-oslc
            // create change --oslc --distribution/--no-distribution

            @Option(
                names = ["--output-urls-file"],
                description = ["Exports URLs created by change create as json."]
            )
            var outputUrlsFile: String? = null
        }

        @ArgGroup(exclusive = false, multiplicity = "0..1", heading = "", order = 1)
        val changeDetails: ChangeDetails = ChangeDetails()

        class ChangeDetails : IDefaultValueProvider { // Inherited for visibility of where defaults are set
            @Option(
                names = ["--gitops"],
                description = ["Uses the environment variable CDLIB_PM_GIT_ORIGIN to identify the associated change in Jira."],
            )
            var gitops = false

            @Option(
                names = ["--start"],
                description = ["Sets the start of the change window (default: Current time)",
                    "Dates formatted according to ISO8601 (i.e. `2023-01-01T00:00:00+01:00`).",
                    "This format can easily be generated with this bash command:  date --iso-8601=s"],
                required = false
            )
            lateinit var startOpt: ZonedDateTime

            @Option(
                names = ["--end"],
                description = ["Sets the end of the change window (default: Current time + 4 hours)",
                    "Dates formatted according to ISO8601 (i.e. `2023-01-01T00:00:00+01:00`).",
                    "This format can easily be generated with this bash command:  date --iso-8601=s -d '+4 hours'"],
                required = false
            )
            lateinit var endOpt: ZonedDateTime

            @Option(
                names = ["--summary"],
                description = ["Changes the summary (title) written to Jira (default: Release name)"],
                required = false,
            )
            lateinit var summary: String

            @Option(
                names = ["--category"],
                description = ["Changes the category written to Jira (default: ROLLOUT)."],
                type = [JiraConstants.Category::class],
                required = false
            )
            lateinit var category: JiraConstants.Category

            @Option(
                names = ["--description"],
                description = ["Changes the description written to Jira (default: Job url)."],
                required = false,
            )
            lateinit var description: String

            @Option(
                names = ["--impact-class"],
                description = ["Changes the impact class written to Jira (default: NONE)."],
                type = [JiraConstants.ImpactClass::class],
                required = false,
            )
            lateinit var impactClass: JiraConstants.ImpactClass

            @Option(
                names = ["--impact"],
                description = ["Changes the impact written to Jira (default: ${JiraConstants.FieldDefaults.IMPACT})"],
                required = false
            )
            lateinit var impact: String

            @Option(
                names = ["--urgency"],
                description = ["Changes the urgency level to Jira (default: LOW)"],
                type = [JiraConstants.Urgency::class],
                required = false
            )
            lateinit var urgency: JiraConstants.Urgency

            @Option(
                names = ["--target"],
                description = ["Changes the target written to Jira (default: ${JiraConstants.FieldDefaults.TARGET})"],
                required = false
            )
            lateinit var target: String

            @Option(
                names = ["--fallback"],
                description = ["Changes the fallback written to Jira (default: ${JiraConstants.FieldDefaults.FALLBACK})"],
                required = false
            )
            lateinit var fallback: String

            @Option(
                names = ["--implementation-risk"],
                description = ["Changes the implementation risk written to Jira (default: ${JiraConstants.FieldDefaults.IMPLEMENTATION_RISK})"],
                required = false
            )
            lateinit var implementationRisk: String

            @Option(
                names = ["--omission-risk"],
                description = ["Changes the omission risk written to Jira (default: ${JiraConstants.FieldDefaults.OMISSION_RISK})"],
                required = false
            )
            lateinit var omissionRisk: String

            @Option(
                names = ["--labels"],
                description = ["Adds the given labels to the Jira issue (default: cdlib). "],
                required = false
            )
            lateinit var labels: String


            @Option(
                names = ["--approval-interval-in-minutes"],
                description = ["Changes the change approval interval check in minutes (default: ${JiraConstants.FieldDefaults.APPROVAL_CHECK_INTERVAL_IN_MINUTES} minutes). "],
                required = false
            )
            var approvalCheckIntervalInMinutes: Int = JiraConstants.FieldDefaults.APPROVAL_CHECK_INTERVAL_IN_MINUTES

            override fun defaultValue(argSpec: Model.ArgSpec): String? {
                if (argSpec.isOption) {
                    val option: Model.OptionSpec = argSpec as Model.OptionSpec
                    val now = ZonedDateTime.now()
                    val isHelp = argSpec.command().findOption("help").getValue<Boolean>()

                    when (option.longestName()) {
                        "--start" -> return now.toString()
                        "--end" -> return now.plusHours(4).toString()
                        "--description" -> return if (isHelp) null else resolveEnvByName(Names.CDLIB_JOB_URL)
                        "--summary" -> return if (isHelp) null else resolveEnvByName(Names.CDLIB_RELEASE_NAME_UNIQUE)
                        "--category" -> return JiraConstants.Category.ROLLOUT.toString()
                        "--impact" -> return JiraConstants.FieldDefaults.IMPACT
                        "--impact-class" -> return JiraConstants.ImpactClass.NONE.toString()
                        "--urgency" -> return JiraConstants.Urgency.LOW.toString()
                        "--target" -> return JiraConstants.FieldDefaults.TARGET
                        "--fallback" -> return JiraConstants.FieldDefaults.FALLBACK
                        "--implementation-risk" -> return JiraConstants.FieldDefaults.IMPLEMENTATION_RISK
                        "--omission-risk" -> return JiraConstants.FieldDefaults.OMISSION_RISK
                        "--labels" -> return ""
                    }
                }
                return null
            }
        }

        @ArgGroup(
            validate = true,
            exclusive = false,
            heading = "Options for WebApprovalSection:%n",
            multiplicity = "0..1",
            order = 2
        )
        var webApprovalSection: WebApprovalSection = WebApprovalSection()

        class WebApprovalSection {
            @Option(
                names = ["--webapproval"],
                description = ["Run the webapproval."],
                negatable = true,
                defaultValue = "true",
                fallbackValue = "true"
            )
            var performWebapproval = true
        }

        @ArgGroup(
            validate = true,
            exclusive = false,
            heading = "Options for TqsSection:%n",
            multiplicity = "0..1",
            order = 4
        )
        var tqsSection: TqsSection = TqsSection()

        class TqsSection {
            @Deprecated("Deprecated due to unsupported plugin and measurements. This flag has no effect and can be removed.")
            @Option(
                names = ["--tqs"],
                description = ["Deprecated due to unsupported plugin and measurements. This flag has no effect and can be removed."],
                negatable = true,
                defaultValue = "false",
                fallbackValue = "true"
            )
            var performTqs = false
        }

        @ArgGroup(
            validate = true,
            exclusive = false,
            heading = "Options for OslcSection:%n",
            multiplicity = "0..1",
            order = 6
        )
        var oslcSection: OslcSection = OslcSection()

        class OslcSection {
            @Option(
                names = ["--oslc"],
                description = ["Run the OSLC reporting."],
                negatable = true,
                defaultValue = "true",
                fallbackValue = "true"
            )
            var performOslc = true

            @Option(
                names = ["--distribution"],
                description = ["Is your project being distributed (and so has harsher requirements for some open source licenses?)",
                    "Can be negated with --no-distribution.",
                    "Required if running OSLC verification."
                ],
                negatable = true,
                defaultValue = "true",
                fallbackValue = "true"
            )
            var isDistribution: Boolean = true
        }

        @Option(
            names = ["--test"],
            description = ["Uses the test environment for Jira and all Sharepoints"],
            required = false
        )
        var isTest = false

        @Mixin
        lateinit var webapprovalMixin: WebapprovalMixin

        @Mixin
        lateinit var artifactoryMixinFull: ArtifactoryMixinFull

        @Mixin
        lateinit var checkMixin: CheckMixin

        @Inject
        lateinit var oslcMixin: OslcMixin

        @Spec
        lateinit var spec: CommandSpec

        @Inject
        lateinit var cosmosDashboardRepository: CosmosDashboardRepository

        @Inject
        lateinit var changeHandler: ChangeHandler

        override fun call(): Int {
            enableDebugIfOptionIsSet()
            val originalArgs = spec.commandLine().parseResult.originalArgs()

            val versionInfo = cosmosDashboardRepository.versionInfo
            if (!versionInfo.isSupported) {
                logger.warn { "CDLib version ${cosmosDashboardRepository.getCdlibVersionViewModel().cdlib} is not supported anymore. Please update to a newer version. Pre-authorization is not possible." }
            }
            if (!versionInfo.isLatest) {
                logger.info { "A new version of CDLib is available" }
            }

            val reportUrl =
                if (webApprovalSection.performWebapproval or oslcSection.performOslc) {
                    logger.info { "Starting release verification..." }
                    if (isTest) {
                        logger.info { "Test run!" }
                    }
                    runCatching {
                        if (oslcSection.performOslc) {
                            logger.info { "Flag --oslc found." }
                            logger.info { "Checking for '--distribution' flag..." }
                            require(originalArgs.any { it.contains("--distribution") or it.contains("--no-distribution") }) {
                                "'--[no-]distribution' is required when running OSLC verification!"
                            }
                            logger.debug { "Flag is set!" }
                        }
                        logger.info { "Verifying reports..." }
                        val nameToAllReports = artifactoryMixinFull.artifactoryReports.also {
                            logger.debug { "Downloaded reports: $it" }
                        }.groupBy { it.name }
                        val nameToBuildReports = artifactoryMixinFull.artifactoryReports.filterNot {
                            it.test.reportType == ReportType.DAST
                        }.groupBy { it.name }

                        if (webApprovalSection.performWebapproval) {
                            logger.info { "Flag --webapproval found." }
                            var hasInvalidReports = false
                            var hasMissingBuildReports = false
                            var hasDastReport = false
                            var hasScaReport = false
                            var hasSastReport = false

                            nameToAllReports.forEach { (name, reports) ->
                                val verificationResult = checkMixin.checkSecurityReports(reports).also {
                                    logger.info { "Verification result for App $name: $it" }
                                }
                                if (verificationResult.hasInvalidReport) {
                                    hasInvalidReports = true
                                    logger.error { "App $name has invalid report(s)!" }
                                }
                                if (!verificationResult.hasSAST && (!verificationResult.hasDAST or (verificationResult.hasDAST and verificationResult.hasSCA))) {
                                    hasMissingBuildReports = true
                                    logger.error { "App $name is missing SAST report!" }
                                }
                                if (!verificationResult.hasSCA && (!verificationResult.hasDAST or (verificationResult.hasDAST and verificationResult.hasSAST))) {
                                    hasMissingBuildReports = true
                                    logger.error { "App $name is missing SCA report!" }
                                }
                                if (verificationResult.hasDAST) {
                                    hasDastReport = true
                                    logger.info { "Found DAST report for App $name." }
                                }
                                if (verificationResult.hasSCA) {
                                    hasScaReport = true
                                }
                                if (verificationResult.hasSAST) {
                                    hasSastReport = true
                                }
                            }
                            if (!hasDastReport) {
                                hasMissingBuildReports = true
                                logger.error { "No DAST report found!" }
                            }
                            if (!hasScaReport) {
                                hasMissingBuildReports = true
                                logger.error { "No SCA report found!" }
                            }
                            if (!hasSastReport) {
                                hasMissingBuildReports = true
                                logger.error { "No SAST report found!" }
                            }

                            check(!hasInvalidReports) {
                                "Invalid report(s)!"
                            }
                            check(!hasMissingBuildReports) {
                                "Missing reports!"
                            }
                            logger.info { "Verifying pipeline approval configuration..." }
                            check(webapprovalMixin.isConfigurationApproved()) {
                                "Failed to validate approval configuration."
                            }
                        }

                        if (tqsSection.performTqs) {
                            logger.info { "Flag --tqs found. Deprecated due to unsupported plugin and measurements. This flag has no effect and can be removed." }
                        }

                        if (oslcSection.performOslc) {
                            val oslcReportAppNames = artifactoryMixinFull.oslcReports.map(Report::name)

                            var hasMissingOSLCReport = false
                            var hasComplianceIssues = false
                            nameToBuildReports.forEach { (name, reports) ->
                                if (name !in oslcReportAppNames) {
                                    hasMissingOSLCReport = true
                                    logger.error { "App $name is missing OSLC report!" }
                                } else {
                                    val oslcReportsForApp = artifactoryMixinFull.oslcReports
                                        .filter { report -> report.name == name }
                                    if (oslcReportsForApp.size > 1) {
                                        logger.warn { "Found multiple OSLC reports for $name" }
                                    }
                                    oslcReportsForApp.forEach { report ->
                                        when (report.test.tool.name) {
                                            Tool.OSLC_FNCI_NAME -> logger.info { "Found OSLC-Report from FNCI for $name" }
                                            Tool.OSLC_MAVEN_PLUGIN_NAME -> logger.info { "Found OSLC-Report from OSLC-Maven-Plugin for $name" }
                                            Tool.OSLC_GRADLE_PLUGIN_NAME -> logger.info { "Found OSLC-Report from OSLC-Gradle-Plugin for $name" }
                                            Tool.OSLC_NPM_PLUGIN_NAME -> logger.info { "Found OSLC-Report from OSLC-NPM-Plugin for $name" }
                                            else -> logger.warn { "Found OSLC-Report from from unkown tool for $name" }
                                        }
                                    }
                                }

                                runCatching {
                                    checkMixin.checkOslcCompliance(name, reports, oslcSection.isDistribution)
                                }.onFailure {
                                    hasComplianceIssues = true
                                    logger.error { it.message }
                                    logger.error { "App $name has compliance issues!" }
                                }
                            }

                            check(!hasMissingOSLCReport) {
                                "Missing OSLC reports!"
                            }

                            check(!hasComplianceIssues) {
                                "There are OSLC compliance issues!"
                            }
                        }

                        logger.info { "Generating and copying immutable artifacts..." }
                        val reportFolderUrl = artifactoryMixinFull.copyFiles()
                        val reports = nameToAllReports.flatMap { it.value }
                        artifactoryMixinFull.uploadSuppressions(reports.map(Report::test).securityTestsSuppressions())
                        cosmosDashboardRepository.addRelease(
                            Release(
                                test = reports.firstOrNull { it.test.reportType == ReportType.DAST },
                                builds = reports.filter { it.test.reportType != ReportType.DAST },
                                reportFolderUrl = reportFolderUrl,
                                cdlibVersionViewModel = cosmosDashboardRepository.getCdlibVersionViewModel()
                            ), isTest
                        )
                        logger.info { "Release verification succeeded!" }
                        reportFolderUrl
                    }.getOrElse {
                        logger.error { "Release verification failed." }
                        it.klogSelf(logger)
                        return -1
                    }
                } else {
                    logger.info { "Skipping release verification." }
                    null
                }

            logger.debug { "CDLib version: $versionInfo" }
            runCatching {
                logger.info { "Starting Change Management process." }
                changeHandler
                    .initialise(
                        authToken = auth,
                        isTestFlag = isTest,
                        skipApprovalWaitFlag = changeManagementSection.skipApprovalWait,
                        enforceFrozenZoneFlag = changeManagementSection.enforceFrozenZone,
                        performWebapprovalFlag = webApprovalSection.performWebapproval,
                        performOslcFlag = oslcSection.performOslc,
                        gitopsFlag = changeDetails.gitops,
                    )
                    .findItSystem(changeManagementSection.commercialReference)
                    .findExisting()

                if (changeManagementSection.resume) {
                    changeHandler
                        .findResumable()
                        .closeExisting()
                        .resume()
                        .comment(changeManagementSection.comment)
                        .monitor(changeDetails.approvalCheckIntervalInMinutes)
                } else {
                    changeHandler
                        .closeExisting()
                        .post(changeDetails)
                        .comment(changeManagementSection.comment)
                        .preauthorize()
                        .transition(OPEN_TO_IMPLEMENTATION)
                        .monitor(changeDetails.approvalCheckIntervalInMinutes)
                }
                logger.info { "Finishing Change Management process." }
            }.onFailure {
                it.klogSelf(logger)
                return -1
            }.getOrThrow()

            val webapprovalUrl = if (webApprovalSection.performWebapproval) {
                logger.info { "Flag --webapproval found. Adding Sharepoint entry." }
                runCatching {
                    checkNotNull(reportUrl)
                    val webapproval = webapprovalMixin.addEntry(reportUrl, artifactoryMixinFull.artifactoryReports)
                    artifactoryMixinFull.uploadJsonable(webapproval)
                    webapproval.url
                }.getOrElse {
                    logger.error { "Failed publishing Webapproval to Sharepoint." }
                    it.klogSelf(logger)
                    return -1
                }
            } else {
                logger.info { "Flag --webapproval not found. Skipping Sharepoint entry." }
                null
            }

            if (tqsSection.performTqs) {
                logger.info { "Flag --tqs found. Deprecated due to unsupported plugin and measurements. This flag has no effect and can be removed." }
            }

            val oslcURls = if (oslcSection.performOslc) {
                logger.info { "Flag --oslc found. Adding OSLC entry." }
                runCatching {
                    oslcMixin.addEntries(
                        isTest,
                        changeHandler.getItSystem(),
                        oslcSection.isDistribution,
                        artifactoryMixinFull.oslcReportsWithMetadata,
                    )
                }.getOrElse {
                    logger.error { "Failed publishing OSLC Reports to Sharepoint" }
                    it.klogSelf(logger)
                    return -1
                }
            } else {
                logger.info { "Flag --oslc not found. Skipping OSLC entry." }
                emptyList()
            }

            changeManagementSection.outputUrlsFile?.let { outputUrlFile ->
                val file = File(outputUrlFile)
                if (file.exists()) {
                    logger.warn { "File $outputUrlFile already exists and will be overriden." }
                }
                logger.info { "Writing generated URLs during change creation to file: ${file.absolutePath}" }
                val urlInfo = ChangeUrlFile(
                    cdlibChangeJiraUrl = changeHandler.getUrl(),
                    cdlibChangeImmutableRepoUrl = reportUrl,
                    cdlibChangeWebapprovalEntryUrl = webapprovalUrl,
                    cdlibChangeOslcEntryUrls = oslcURls,
                    cdlibChangeTqsEntryUrls = emptyList() // TODO: Left here to not break sharepoint model. Remove in 7.0 or find way to include cdlib-external sonar url?
                )
                runCatching {
                    defaultObjectMapper.writeValue(file, urlInfo)
                }.onSuccess {
                    logger.info { "Write successful." }
                }.onFailure {
                    logger.error { "Failed to write file!" }
                    it.klogSelf(logger)
                    return -1
                }
            }

            return 0
        }

        companion object : KLogging()
    }

    @Command(
        name = "close",
        description = ["Publishes the metrics to devops.deutschepost.de"],
    )
    class CloseCommand : SubcommandWithHelp(), Callable<Int> {

        @Inject
        lateinit var changeManagementRepository: ChangeManagementRepository

        @Inject
        lateinit var cosmosDashboardRepository: CosmosDashboardRepository

        private val auth: String by lazy { "Bearer $token" }

        @Option(
            names = ["--commercial-reference"],
            description = ["Number of the commercial reference (i.e. Leistungsschein) to use with the change management API."],
            required = true
        )
        lateinit var commercialReference: String

        @Option(
            names = ["--status"],
            required = true,
            description = [
                "Current build status of the pipeline.",
            ]
        )
        lateinit var statusStr: String

        @Option(
            names = ["--token", "--jira-token"],
            description = ["Personal Access Token (Bearer) for the Change Management Jira API."],
            required = true
        )
        lateinit var token: String

        @Option(
            names = ["--comment"],
            description = ["Adds the given comment to the Jira issue. "],
            required = false,
            defaultValue = ""
        )
        lateinit var comment: String

        @Option(
            names = ["--test"],
            required = false,
            description = ["Change Management: Uses the test environment Jira API. Webapproval: Looks for files in test folders."]
        )
        var isTest = false

        @Option(
            names = ["--deployment-type"],
            description = ["The type of your deployment. ", "One of APP,INFRA | Default: APP"]
        )
        var deploymentType = Deployment.DeploymentType.APP

        @Option(
            names = ["--gitops"],
            description = ["Uses the environment variable CDLIB_PM_GIT_ORIGIN to identify the associated change in Jira."],
        )
        var gitops = false

        @Option(
            names = ["--artifactory-api-key", "--artifactory-identity-token"],
            description = ["Artifactory Identity Token according to CDlib tutorial"]
        )
        var artifactoryIdentityToken: String = ""

        @ArgGroup(
            validate = true,
            exclusive = true,
            heading = "Options for Artifactory Instances",
            multiplicity = "0..1",
            order = 8
        )
        var artifactoryInstanceSection = ArtifactoryInstanceSection()

        val client by lazy {
            ArtifactoryClient(
                apiKeyOrIdentityToken = artifactoryIdentityToken,
                artifactoryUrl = artifactoryInstanceSection.selectArtifactoryUrl()
            )
        }

        @Option(names = ["--immutable-repo-name"], description = ["Name of the repository inside Artifactory."])
        var immutableRepoName: String = ""


        private val artifactoryFolder by lazy {
            if (artifactoryIdentityToken.isNotBlank() && immutableRepoName.isNotBlank()) {
                buildString {
                    append(resolveEnvByName(Names.CDLIB_RELEASE_NAME_UNIQUE))
                    if (isTest) {
                        append("_TEST")
                    }
                }
            } else {
                null
            }
        }

        override fun call(): Int {
            enableDebugIfOptionIsSet()

            val itSystem = runCatching {
                logger.info { "Retrieving IT system information (commercial reference, ALM-ID, name, key)." }
                changeManagementRepository.getItSystem(commercialReference, auth)
            }.getOrElse {
                it.klogSelf(logger)
                logger.error { "Could not find IT system information, did you specify the commercial reference (i.e. Leistungsschein)?" }
                return -1
            }

            val status = parseStatus(statusStr) ?: return -1

            val changeToClose = findChangeForPipeline(itSystem.key) ?: return -1
            val labels = changeToClose.labels
            val artifactoryRequiredLabels = setOf(
                Labels.CHANGE_WEBAPPROVAL,
                Labels.CHANGE_OSLC
            )

            770: if (labels.any { label -> label in artifactoryRequiredLabels } && (immutableRepoName.isEmpty() || artifactoryIdentityToken.isEmpty())) {
            771: logger.error { "The change for this pipeline has at least one of the following labels: webapproval, oslc. Therefore it is mandatory to supply the parameters: --artifactory-identity-token and --immutable-repo-name" }
            772: return -1
            773: }

            if (status.value != "SUCCESS") {
                logger.info { "Status was '$statusStr', not closing change request." }
            } else {
                logger.info { "Closing change ${changeToClose.key}" }
                closeChange(changeToClose) ?: return -1
            }
            val changeAfterClose = changeManagementRepository.getChangeRequest(changeToClose.id, auth)

            runCatching {
                val deployment = generateDeploymentMetrics(changeAfterClose, itSystem, status, labels)
                publishMetrics(deployment)
            }.getOrElse {
                logger.error { "Failed to publish metric object." }
                it.klogSelf(logger)
            }
            return 0
        }

        private fun parseStatus(status: String): Deployment.Status? {
            return runCatching {
                Deployment.Status.valueOf(status.uppercase())
            }.getOrElse {
                logger.error { "Failed to parse Pipeline status: $status\n${it.message}" }
                return null
            }
        }

        private fun findChangeForPipeline(itSystemKey: String?): Change? {
            logger.info { "Finding change to close for the current pipeline." }
            logger.debug { "Setting pipeline url for query." }
            val externalReferenceTwo =
                if (gitops) resolveEnvByName(Names.CDLIB_PM_GIT_ORIGIN) else resolveEnvByName(Names.CDLIB_PIPELINE_URL)
            logger.debug { "Building query object." }
            val request = GetChangesRequest(externalReferenceTwo, itSystemKey, isTest)
            return runCatching {
                logger.info { "Searching changes with the jql query: \n${request.jql}" }
                val changes = changeManagementRepository.getChangeRequestsIssues(auth, request)
                when {
                    changes.isEmpty() ->
                        throw UnsupportedOperationException("Could not find change to close for the current pipeline. Aborting pipeline run...")

                    changes.count() > 1 -> {
                        val message = buildString {
                            append("Found multiple changes for this pipeline, please close them manually using the links below: ")
                            changes.forEachIndexed { index, it ->
                                append("\n  ${index + 1}: ${it.self}")
                            }
                            append("\nAborting pipeline run...")
                        }
                        throw UnsupportedOperationException(message)
                    }

                    else -> {
                        val change = changes.single()
                        logger.info { "Found the following change to close: ${change.self}" }
                        change
                    }
                }
            }.onFailure {
                logger.error { "${it.message}" }
                return null
            }.getOrThrow()
        }

        private fun closeChange(changeToClose: Change): Boolean? {
            logger.info { "Closing change ${changeToClose.key}" }
            logger.info { "Closing change request." }
            val now = ZonedDateTime.now()
            requireNotNull(changeToClose.id)

            val fields = JiraTransitionFields(
                completionCodeField = CompletionCodeField(JiraConstants.CompletionCode.SUCCESS.value),
                startOfImplementation = now.minusSeconds(-5),
                endOfImplementation = now
            )
            val transitionComment = JiraTransitionUpdate("Change wurde durchgeführt.")

            return runCatching {
                logger.info { "Transitioning to next change request phase." }
                changeManagementRepository.transitionChangePhase(
                    changeToClose.id,
                    IMPLEMENTATION_TO_IN_PROGRESS.value,
                    auth
                )
                changeManagementRepository.transitionChangePhase(
                    changeToClose.id,
                    REVIEW.value,
                    auth,
                    fields,
                    transitionComment
                )

                if (comment.isNotEmpty()) {
                    logger.info { "Adding custom comment to change." }
                    changeManagementRepository.addComment(id = changeToClose.id, comment = comment, auth = auth)
                }

                logger.info { "Change request phase transitioned successfully: 'Under Review'. Change was implemented successfully and is to be reviewed." }
                true
            }.getOrElse {
                logger.error { "Could not transition to 'Under Review'. \nError: ${it.message}" }
                null
            }
        }

        private fun generateDeploymentMetrics(
            change: Change,
            itSystem: ItSystem,
            status: Deployment.Status,
            labels: List<String>
        ): Deployment {
            val release = getReleaseFromRepo(deploymentType)
            val deploymentLeadDuration = getDeploymentLeadDuration(deploymentType, release)
            val webapproval = getWebapprovalFromRepo(deploymentType, labels)
            return Deployment(
                cdlibData = cosmosDashboardRepository.getCdlibVersionViewModel(),
                deploymentType = deploymentType.name,
                release = release,
                status = status,
                itSystem = itSystem,
                change = change,
                gitops = gitops,
                deploymentLeadTimeInSeconds = deploymentLeadDuration?.seconds,
                webapproval = webapproval,
                hasWebapproval = Labels.CHANGE_WEBAPPROVAL in labels,
                hasTqs = Labels.CHANGE_TQS in labels,
                hasOslc = Labels.CHANGE_OSLC in labels,
                isTest = isTest
            ).also {
                logger.info { "Pushing following metric object:\n${it.toPrettyString()}" }
            }
        }

        private fun publishMetrics(deployment: Deployment) {
            val isCosmosAvailable = cosmosDashboardRepository.testConnection()
            if (!isCosmosAvailable) {
                logger.error {
                    """Please request firewall clearance to cdlib-dashboard via:
                         | https://git.dhl.com/CDLib/CDlib/issues/new?assignees=ab6jg8%2C+omh9ote011&labels=%3Afire%3A+%3Akey%3A+Firewall&template=firewall-request.md&title=firewall+request+XYZ
                        """.trimMargin()
                }
            }

            require(isCosmosAvailable) {
                "Unable to push to Azure Cosmosdb DevopsDashboard."
            }

            logger.info { "Publishing metrics to Azure CosmosDB..." }
            cosmosDashboardRepository.addDeployment(deployment, isTest)
        }

        private fun getReleaseFromRepo(deploymentType: Deployment.DeploymentType): Release? =
            when {
                deploymentType == Deployment.DeploymentType.INFRA -> null

                else -> {
                    runCatching {
                        cosmosDashboardRepository.getRelease(resolveEnvByName(Names.CDLIB_RELEASE_NAME_UNIQUE), isTest)
                    }.getOrElse {
                        Release(cdlibVersionViewModel = cosmosDashboardRepository.getCdlibVersionViewModel()).also {
                            logger.warn { "Couldn't find release in artifactory. Using fallback for reporting." }
                            logger.warn { "This fallback will be removed in a future release." }
                            logger.debug { it }
                        }
                    }
                }
            }

        private fun getDeploymentLeadDuration(deploymentType: Deployment.DeploymentType, release: Release?): Duration? {
            if (deploymentType == Deployment.DeploymentType.INFRA) {
                return null
            }

            val deploymentLeadDuration = release?.builds?.maxOfOrNull {
                it.date
            }?.let {
                Duration.between(it, ZonedDateTime.now())
            }
            if (deploymentLeadDuration == null) {
                logger.warn { "Could NOT calculate Deployment Lead Time!" }
            } else {
                logger.info { "Deployment Lead Time: ${deploymentLeadDuration.toKotlinDuration()}" }
            }

            return deploymentLeadDuration
        }

        private fun getWebapprovalFromRepo(
            deploymentType: Deployment.DeploymentType,
            artifactoryLabels: List<String>
        ): Webapproval? =
            when {
                deploymentType == Deployment.DeploymentType.INFRA -> null

                Labels.CHANGE_WEBAPPROVAL !in artifactoryLabels -> null

                artifactoryFolder != null -> {
                    val webapprovalFilepath = "$artifactoryFolder/${Webapproval.canonicalFilename()}"
                    val inputStream =
                        client.downloadLatestFile(immutableRepoName, webapprovalFilepath)
                    if (inputStream != null) {
                        logger.info { "Downloaded Webapproval from $webapprovalFilepath" }
                        defaultObjectMapper.readValue(inputStream, Webapproval::class.java)
                    } else {
                        logger.error { "Could not download Webapproval from $webapprovalFilepath" }
                        null
                    }
                }

                else -> null
            }

        companion object : KLogging()
    }
}
