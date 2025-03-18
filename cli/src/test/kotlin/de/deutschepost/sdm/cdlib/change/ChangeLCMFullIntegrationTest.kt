package de.deutschepost.sdm.cdlib.change

import de.deutschepost.sdm.cdlib.artifactory.Artifactory
import de.deutschepost.sdm.cdlib.CdlibCommand
import de.deutschepost.sdm.cdlib.artifactory.AZURE_ARTIFACTORY_URL
import de.deutschepost.sdm.cdlib.artifactory.ArtifactoryClient
import de.deutschepost.sdm.cdlib.change.changemanagement.ChangeTestHelper
import de.deutschepost.sdm.cdlib.change.changemanagement.api.ChangeHandler
import de.deutschepost.sdm.cdlib.change.metrics.client.CosmosDashboardRepository
import de.deutschepost.sdm.cdlib.names.Names
import de.deutschepost.sdm.cdlib.utils.*
import getSystemEnvironmentTestListenerWithOverrides
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.spec.style.FunSpec
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.*
import toArgsArray
import withStandardOutput
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// TODO: Remove after Sundown

@RequiresTag("IntegrationTest")
@Tags("IntegrationTest")
@MicronautTest
class ChangeLCMFullIntegrationTest(
    @Value("\${change-management-token}") val token: String,
    @MicronautTest
    class ChangeLCMFullIntegrationTest(
        @Value("\${change-management-token}") val token: String,
        @Value("\${sharepoint.username}") val sp_username: String,
        @Value("\${sharepoint.password}") val sp_password: String,
        @Value("\${artifactory-azure-identity-token}") val artifactoryLCMIdentityToken: String,
        private val changeTestHelper: ChangeTestHelper,
        private val changeHandler: ChangeHandler,
        private val cosmosDashboardRepository: CosmosDashboardRepository
    ) : FunSpec() {
        private val timestamp =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        private val appName = "cli"
        private val releaseName = "$appName-Integration-Release"
        private val releaseNameUnique = "${releaseName}_$timestamp"
        private var owaspName = "Integration-owasp-$timestamp"
        private var fortifyName = "Integration-fortify-$timestamp"
        private var zapName = "Integration-zap-$timestamp"
        private var genericReportName = "Integration-generic-$timestamp"
        private var ccaName = "Integration-cca-$timestamp"
        private var oslcName = "Integration-oslc-$timestamp"
        private val repoName = "ICTO-3339_sdm_sockshop_release_reports"
        private val immutableRepoName = "ICTO-3339_sdm_sockshop_nonimmutable_reports"
        private val artifactoryClient = ArtifactoryClient(artifactoryLCMIdentityToken, AZURE_ARTIFACTORY_URL)
        private val cdlibApplicationId = 5
        private val jenkinsJobUrl = "https://integration-test-url.jenkuns.example.com/foo/bar/job/1337"
        private val artifactory by lazy {
            val declaredField = artifactoryClient.javaClass.getDeclaredField("artifactory")
            declaredField.isAccessible = true
    private val artifactory = declaredField.get(artifactoryClient) as Artifactory
    @Value("\${artifactory-azure-identity-token}") val artifactoryLCMIdentityToken: String,
    private val changeTestHelper: ChangeTestHelper,
    private val changeHandler: ChangeHandler,
    private val cosmosDashboardRepository: CosmosDashboardRepository
) : FunSpec() {
    private val timestamp =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS))
    private val appName = "cli"
    private val releaseName = "$appName-Integration-Release"
    private val releaseNameUnique = "${releaseName}_$timestamp"
    private var owaspName = "Integration-owasp-$timestamp"
    private var fortifyName = "Integration-fortify-$timestamp"
    private var zapName = "Integration-zap-$timestamp"
    private var genericReportName = "Integration-generic-$timestamp"
    private var ccaName = "Integration-cca-$timestamp"
    private var oslcName = "Integration-oslc-$timestamp"
    private val repoName = "ICTO-3339_sdm_sockshop_release_reports"
    private val immutableRepoName = "ICTO-3339_sdm_sockshop_nonimmutable_reports"
    private val artifactoryClient = ArtifactoryClient(artifactoryLCMIdentityToken, AZURE_ARTIFACTORY_URL)
    private val cdlibApplicationId = 5
    private val jenkinsJobUrl = "https://integration-test-url.jenkuns.example.com/foo/bar/job/1337"
    private val artifactory by lazy {
        val declaredField = artifactoryClient.javaClass.getDeclaredField("artifactory")
        declaredField.isAccessible = true
        declaredField.get(artifactoryClient) as Artifactory
    }
    private lateinit var changeDetails: ChangeCommand.CreateCommand.ChangeDetails
    private val outputUrlFile = "change-full-int.json"

    override suspend fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        changeDetails = changeTestHelper.changeDetailsWithDefaults()
    }

    private val envVariables = mutableMapOf(
        "CDLIB_RELEASE_NAME_UNIQUE" to releaseNameUnique,
        "CDLIB_RELEASE_NAME" to releaseName,
        "CDLIB_APP_NAME" to appName,
        "CDLIB_APP_VERSION" to "1337",
        "CDLIB_JOB_URL" to jenkinsJobUrl,
        "CDLIB_PM_GIT_MAIL" to "integration-test-git-mail",
        "CDLIB_PM_GIT_NAME" to "integration-test-git-author",
        "CDLIB_PM_GIT_ID" to "integration-test-git-id",
        "CDLIB_PM_GIT_LINK" to "integration-test-git-link",
        "CDLIB_PM_GIT_MESSAGE" to "integration-test-git-message",
        "CDLIB_PM_GIT_ORIGIN" to "integration-test-git-origin",
        "CDLIB_CICD_PLATFORM" to "integration-test-platform",
        //"CDLIB_JOB_URL" to "integration-test-platform.com/integration-test",
    )

    override fun listeners() = listOf(
        getSystemEnvironmentTestListenerWithOverrides(
            envVariables + mapOf(
                "BUILD_URL" to "test build url: https://integrationtest.jenkinsbuildurl.de"
            )
        )
    )

    init {
        context("Creating webapproval change is a success") {
            lateinit var output: String
            test("Command runs without exception for LCM") {
                val args =
                    ("change create --jira-token $token --debug " +
                        "--commercial-reference 5296 --test --skip-approval-wait --no-distribution " +
                        "--artifactory-azure-instance --artifactory-identity-token $artifactoryLCMIdentityToken --repo-name $repoName " +
                        "--folder-name $fortifyName --folder-name $owaspName --folder-name $zapName " +
                        "--folder-name $oslcName --sharepoint-username $sp_username --sharepoint-password $sp_password " +
                        "--immutable-repo-name $immutableRepoName --application-id $cdlibApplicationId --oslc --output-urls-file $outputUrlFile").toArgsArray()

                output = withStandardOutput {
                    PicocliRunner.run(CdlibCommand::class.java, *args)
                }.second
            }
        }
    }

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        unmockkAll()
        unmockkObject(changeHandler)
    }

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)

        mockkStatic("de.deutschepost.sdm.cdlib.utils.EnvUtilsKt")
        envVariables.forEach { (key, value) ->
            every { resolveEnvByName(Names.valueOf(key)) } returns value
        }

        val reportFolder = uploadReportsToArtifactory(
            artifactoryClient, repoName,
            fortifyName = fortifyName,
            owaspName = owaspName,
            zapName = zapName,
            ccaName = ccaName,
            genericReportName = genericReportName,
            oslcFNCIName = oslcName
        )
        fortifyName = reportFolder.fortifyName
        owaspName = reportFolder.owaspName
        zapName = reportFolder.zapName
        ccaName = reportFolder.ccaName
        genericReportName = reportFolder.genericReportName
        oslcName = reportFolder.oslcFNCIName

        File(outputUrlFile).delete()

        unmockkStatic("de.deutschepost.sdm.cdlib.utils.EnvUtilsKt")
        unmockkAll()

        // Mocking Version validation due to issues of CosmosDB that occur when using withConstantNow
        mockCosmosDBVersionInfo()
        mockCosmosDBClient()
        mockChangeHandler(changeHandler)
    }
}
