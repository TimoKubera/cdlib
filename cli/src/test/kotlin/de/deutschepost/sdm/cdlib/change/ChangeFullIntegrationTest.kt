package de.deutschepost.sdm.cdlib.change

import de.deutschepost.sdm.cdlib.CdlibCommand
import de.deutschepost.sdm.cdlib.artifactory.ARTIFACTORY_GUI
import de.deutschepost.sdm.cdlib.artifactory.ArtifactoryClient
import de.deutschepost.sdm.cdlib.artifactory.ITS_ARTIFACTORY_URL
import de.deutschepost.sdm.cdlib.change.changemanagement.ChangeTestHelper
import de.deutschepost.sdm.cdlib.change.changemanagement.api.ChangeHandler
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants
import de.deutschepost.sdm.cdlib.change.metrics.client.CosmosDashboardRepository
import de.deutschepost.sdm.cdlib.change.metrics.model.ChangeUrlFile
import de.deutschepost.sdm.cdlib.names.Names
import de.deutschepost.sdm.cdlib.utils.*
import exists
import getSystemEnvironmentTestListenerWithOverrides
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.withClue
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.*
import org.jfrog.artifactory.client.Artifactory
import toArgsArray
import withStandardOutput
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@RequiresTag("IntegrationTest")
@Tags("IntegrationTest")
@MicronautTest
class ChangeFullIntegrationTest(
    @Value("\${change-management-token}") val token: String,
    @Value("\${sharepoint.username}") val spUsername: String,
    @Value("\${sharepoint.password}") val spPassword: String,
    @Value("\${artifactory-its-identity-token}") val artifactoryIdentityToken: String,
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
    private val repoName = "sdm-proj-prg-cdlib-cli-appimage"
    private val immutableRepoName = "sdm-proj-prg-cdlib-cli-appimage"
    private val artifactoryClient = ArtifactoryClient(artifactoryIdentityToken, ITS_ARTIFACTORY_URL)
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
            test("Command runs without exception") {
                val args =
                    ("change create --jira-token $token --debug " +
                        "--commercial-reference 5296 --test --skip-approval-wait --no-distribution " +
                        "--artifactory-its-instance --artifactory-identity-token $artifactoryIdentityToken --repo-name $repoName " +
                        "--folder-name $fortifyName --folder-name $owaspName --folder-name $zapName " +
                        "--folder-name $oslcName --sharepoint-username $sp_username --sharepoint-password $sp_password " +
                        "--immutable-repo-name $immutableRepoName --application-id $cdlibApplicationId --oslc --output-urls-file $outputUrlFile").toArgsArray()

                output = withStandardOutput {
                    PicocliRunner.run(CdlibCommand::class.java, *args)
                }.second
            }

            test("Reports exist in artifactory") {
                val repo = artifactory.repository(immutableRepoName)
                listOf(fortifyName, owaspName, zapName, oslcName).forEach {
                    withClue(it) {
                        repo.folder("${releaseNameUnique}_TEST/$it").exists() shouldBe true
                    }
                }
            }
            test("Files exist in artifactory") {
                val repo = artifactory.repository(immutableRepoName)
                repo.folder("${releaseNameUnique}_TEST").exists() shouldBe true
                repo.file("${releaseNameUnique}_TEST/security_suppressions.json").exists() shouldBe true
                output shouldContain "was not sanitized."
            }

            test("Finds Release Info in Cosmos DB") {
                val releaseInfo = cosmosDashboardRepository.getRelease(releaseNameUnique, true)
                withClue(releaseInfo) {
                    releaseInfo shouldNotBe null
                    releaseInfo.releaseName shouldBe releaseName
                }
            }

            test("Finds --test param") {
                output shouldContain "iShare test list!"
            }
            test("Successful Webapproval Sharepoint entry") {
                val immutableRepoUrl =
                    "$ITS_ARTIFACTORY_URL$ARTIFACTORY_GUI/$immutableRepoName/${releaseNameUnique}_TEST"
                output shouldContain "Successfully copied artifacts to immutable repository."
                output shouldContain "ArtifactsUrl: $immutableRepoUrl"
                output shouldContain "Getting Sharepoint Digest: HTTP/1.1 200 OK"
                output shouldContain "Adding Record: HTTP/1.1 201 Created"
                output shouldContain "\"identifier\":\"${releaseNameUnique}\""
                output shouldContain "\"report_x002d_url\":\"$immutableRepoUrl\""
                output shouldContain "\"job_x002d_url\":\"${jenkinsJobUrl}\""
            }
            test("Successfully written output file") {
                output shouldContain "Writing generated URLs during change creation to file"
                output shouldContain "Write successful"
                val file = File(outputUrlFile)
                file.exists() shouldBe true
                val changeUrlFile = shouldNotThrowAny {
                    defaultObjectMapper.readValue(file.inputStream(), ChangeUrlFile::class.java)
                }
                withClue(changeUrlFile) {
                    changeUrlFile.cdlibChangeImmutableRepoUrl shouldNotBe null
                    changeUrlFile.cdlibChangeWebapprovalEntryUrl shouldNotBe null
                    changeUrlFile.cdlibChangeOslcEntryUrls.size shouldBeGreaterThanOrEqual 1
                }
            }
            test("Change closure after full integration test should fail with missing --artifactory-identity-token") {
                changeHandler
                    .initialise(
                        "Bearer $token",
                        isTestFlag = true,
                        enforceFrozenZoneFlag = false,
                        performWebapprovalFlag = true,
                        performOslcFlag = false,
                        gitopsFlag = false,
                        skipApprovalWaitFlag = true
                    )
                    .findItSystem("5296")
                    .post(changeDetails)
                    .preauthorize()
                    .transition(JiraConstants.ChangePhaseId.OPEN_TO_IMPLEMENTATION)
                val args: Array<String> =
                    ("--test --debug --jira-token $token --commercial-reference 5296 --immutable-repo-name $immutableRepoName --status UNSTABLE").toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(ChangeCommand.CloseCommand::class.java, *args)
                }
                output shouldContain "The change for this pipeline has at least one of the following labels: webapproval, oslc. Therefore it is mandatory to supply the parameters: --artifactory-identity-token and --immutable-repo-name"
                ret shouldBe -1
            }

            test("Change closure after full integration test should fail with missing --immutable-repo-name $immutableRepoName") {
                changeHandler
                    .initialise(
                        "Bearer $token",
                        isTestFlag = true,
                        enforceFrozenZoneFlag = false,
                        performWebapprovalFlag = true,
                        performOslcFlag = false,
                        gitopsFlag = false,
                        skipApprovalWaitFlag = true
                    )
                    .findItSystem("5296")
                    .post(changeDetails)
                    .preauthorize()
                    .transition(JiraConstants.ChangePhaseId.OPEN_TO_IMPLEMENTATION)
                val args: Array<String> =
                    ("--test --debug --jira-token $token --commercial-reference 5296 --artifactory-its-instance --artifactory-identity-token $artifactoryIdentityToken --status UNSTABLE").toArgsArray()
                val (ret, _) = withStandardOutput {
                    PicocliRunner.call(ChangeCommand.CloseCommand::class.java, *args)
                }
                ret shouldBe -1
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
