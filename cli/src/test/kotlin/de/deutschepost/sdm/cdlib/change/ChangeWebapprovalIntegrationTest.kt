package de.deutschepost.sdm.cdlib.change

import de.deutschepost.sdm.cdlib.CdlibCommand
import de.deutschepost.sdm.cdlib.artifactory.ARTIFACTORY_ARCHIVE_TYPE_PROPERTY
import de.deutschepost.sdm.cdlib.artifactory.ARTIFACTORY_GUI
import de.deutschepost.sdm.cdlib.artifactory.ArtifactoryClient
import de.deutschepost.sdm.cdlib.artifactory.ITS_ARTIFACTORY_URL
import de.deutschepost.sdm.cdlib.change.metrics.client.CosmosDashboardRepository
import de.deutschepost.sdm.cdlib.utils.mockCosmosDBClient
import de.deutschepost.sdm.cdlib.utils.mockCosmosDBVersionInfo
import de.deutschepost.sdm.cdlib.utils.uploadReportsToArtifactory
import exists
import getSystemEnvironmentTestListenerWithOverrides
import io.kotest.assertions.withClue
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.withEnvironment
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.unmockkAll
import org.jfrog.artifactory.client.Artifactory
import toArgsArray
import withStandardOutput
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

@RequiresTag("IntegrationTest")
@Tags("IntegrationTest")
@MicronautTest
class ChangeWebapprovalIntegrationTest(
    @Value("\${change-management-token}") val token: String,
    @Value("\${sharepoint.username}") val spUsername: String,
    @Value("\${sharepoint.password}") val sharepointPassword: String,
    @Value("\${artifactory-its-identity-token}") val artifactoryIdentityToken: String,
    private val cosmosDashboardRepository: CosmosDashboardRepository
) : FunSpec() {
    private val appName = "cli"
    private val timestamp =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS))
    private val releaseName = "Integration-Release"
    private var releaseNameUnique = "${releaseName}_$timestamp"
    private var owaspName = "Integration-owasp-$timestamp"
    private var fortifyName = "Integration-fortify-$timestamp"
    private var zapName = "Integration-zap-$timestamp"
    private var genericReportName = "Integration-generic-$timestamp"
    private var ccaName = "Integration-cca-$timestamp"
    private val repoName = "sdm-proj-prg-cdlib-cli-appimage"
    private val immutableRepoName = "sdm-proj-prg-cdlib-cli-appimage"
    private val artifactoryClient = ArtifactoryClient(artifactoryIdentityToken, ITS_ARTIFACTORY_URL)
    private val cdlibApplicationId = 5
    private val jenkinsJobUrl = "https://integration-test-url.jenkuns.example.com/foo/bar/job/1337"
    private val pipelineUrl = UUID.randomUUID().toString()
    private val artifactory by lazy {
        val declaredField = artifactoryClient.javaClass.getDeclaredField("artifactory")
        declaredField.isAccessible = true
        declaredField.get(artifactoryClient) as Artifactory
    }

    private val envVariables = mutableMapOf(
        //"BUILD_URL" to "test build url: https://integrationtest.jenkinsbuildurl.de",
        "CDLIB_RELEASE_NAME_UNIQUE" to releaseNameUnique,
        "CDLIB_RELEASE_NAME" to releaseName,
        "CDLIB_JOB_URL" to jenkinsJobUrl,
        "CDLIB_PIPELINE_URL" to pipelineUrl,
        "CDLIB_APP_NAME" to appName,
        "CDLIB_APP_VERSION" to timestamp,
        "CDLIB_PM_GIT_MAIL" to "integration-test-git-mail",
        "CDLIB_PM_GIT_NAME" to "integration-test-git-author",
        "CDLIB_PM_GIT_ID" to "integration-test-git-id",
        "CDLIB_PM_GIT_LINK" to "integration-test-git-link",
        "CDLIB_PM_GIT_MESSAGE" to "integration-test-git-message",
        "CDLIB_PM_GIT_ORIGIN" to "integration-test-git-origin",
        "CDLIB_CICD_PLATFORM" to "integration-test-platform",
    )

    override fun listeners() = listOf(
        getSystemEnvironmentTestListenerWithOverrides(
            envVariables
        )
    )

    init {
        context("Creating webapproval change is a success") {
            lateinit var output: String
            val args: Array<String> =
                ("change create --jira-token $token --debug " +
                    "--commercial-reference 5296 --test --webapproval --no-tqs --no-oslc " + "--artifactory-its-instance --artifactory-identity-token $artifactoryIdentityToken --repo-name $repoName " +
                    "--folder-name $fortifyName --folder-name $owaspName --folder-name $zapName " + "--sharepoint-username $sp_username --sharepoint-password $sp_password " +
                    "--immutable-repo-name $immutableRepoName --application-id $cdlibApplicationId").toArgsArray()
            test("Command runs without exception") {
                output = withStandardOutput {
                    PicocliRunner.run(CdlibCommand::class.java, *args)
                }.second
            }
            test("Reports exist in artifactory") {
                val repo = artifactory.repository(immutableRepoName)
                listOf(fortifyName, owaspName, zapName).forEach {
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
            test("Correct Tags are set in change") {
                output shouldContain "labels=[cdlib, test, webapproval]"
            }
            test("Successful Sharepoint entry") {
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
            test("Second change create should detect already existing files but it should not fail") {
                val (_, outputSecond) = withStandardOutput {
                    PicocliRunner.run(CdlibCommand::class.java, *args)
                }
                listOf(fortifyName, owaspName, zapName).forEach {
                    val repo = artifactory.repository(immutableRepoName)
                    withClue(it) {
                        repo.folder("${releaseNameUnique}_TEST/$it/$it").exists() shouldBe false
                    }
                }
                outputSecond shouldContain "does exist and checksum matches"
                outputSecond shouldNotContain "does exist and checksum does not match"
                val releaseInfo = cosmosDashboardRepository.getRelease(releaseNameUnique, true)
                withClue(releaseInfo) {
                    releaseInfo shouldNotBe null
                    releaseInfo.releaseName shouldBe releaseName
                }
            }
            test("Third change create should fail because of corrupted files in immutable repo") {
                // Write test file to srcRepo and also a corrupted file in dstRep as test-preparation
                val byteArray1 = ByteArray(
                    size = 1024,
                    init = { _ -> 0xFF.toByte() }) // arbitrary test values for source-file
                val byteArray2 = ByteArray(
                    size = 1024,
                    init = { _ -> 0x55.toByte() }) // different arbitrary test values to simulate corruption of files
                val corruptedFolderName = "corruptionTest"
                val srcFilePath = "$corruptedFolderName/testFile.txt"
                val dstFilePath = "${releaseNameUnique}_TEST/$corruptedFolderName/testFile.txt"
                val srcRepo = artifactory.repository(repoName)
                val dstRepo = artifactory.repository(immutableRepoName)
                val propHandler = srcRepo.folder(corruptedFolderName).properties()
                propHandler.addProperty(ARTIFACTORY_ARCHIVE_TYPE_PROPERTY, "BUILD")
                propHandler.doSet()

                srcRepo
                    .upload(srcFilePath, byteArray1.inputStream())
                    .doUpload()
                dstRepo
                    .upload(dstFilePath, byteArray2.inputStream())
                    .doUpload()

                val argsCreate: Array<String> =
                    ("change create --jira-token $token --debug " +
                        "--commercial-reference 5296 --test --webapproval --no-tqs --no-oslc " +
                        "--artifactory-its-instance --artifactory-identity-token $artifactoryIdentityToken --repo-name $repoName " +
                        "--folder-name $fortifyName --folder-name $owaspName --folder-name $zapName --folder-name $corruptedFolderName " +
                        "--sharepoint-username $sp_username --sharepoint-password $sp_password " +
                        "--immutable-repo-name $immutableRepoName --application-id $cdlibApplicationId").toArgsArray()
                val (_, outputCreate) = withStandardOutput {
                    PicocliRunner.run(CdlibCommand::class.java, *argsCreate)
                }
                outputCreate shouldContain "does exist and checksum matches"
                outputCreate shouldContain "does exist and checksum does not match"
                outputCreate shouldContain "Release verification failed"
            }
            test("Change closure after webapproval is a success") {
                val args: Array<String> =
                    ("--test --debug --jira-token $token --commercial-reference 5296 --artifactory-its-instance --artifactory-identity-token $artifactoryIdentityToken --immutable-repo-name $immutableRepoName --status UNSTABLE").toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(ChangeCommand.CloseCommand::class.java, *args)
                }

                output shouldContain "Pushing following metric object"
                output shouldContain "APP"
                ret shouldBe 0
            }
        }

        withEnvironment(envVariables + mapOf("CDLIB_APP_NAME" to "appName_TEST"), OverrideMode.SetOrOverride) {
            val reportFolder = uploadReportsToArtifactory(
                artifactoryClient, repoName,
                genericReportName = genericReportName
            )
            genericReportName = reportFolder.genericReportName
        }

        context("Creating webapproval change is a failure") {
            test("Missing Reports in webapproval verification") {
                val args: Array<String> =
                    ("--jira-token $token --debug " +
                        "--commercial-reference 5296 --test --webapproval --no-tqs --no-oslc --artifactory-its-instance --artifactory-identity-token $artifactoryIdentityToken --repo-name $repoName " +
                        "--folder-name $fortifyName --folder-name $owaspName --folder-name $zapName --sharepoint-username $sp_username --sharepoint-password $sp_password " +
                        "--folder-name $genericReportName --immutable-repo-name $immutableRepoName --application-id $cdlibApplicationId").toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(ChangeCommand.CreateCommand::class.java, *args)
                }
                output shouldNotContain "No DAST report found!"
                output shouldContain "App appName_TEST is missing SAST report!"
                output shouldContain "App appName_TEST is missing SCA report!"
                output shouldContain "Missing reports!"
                ret shouldBeExactly -1
            }
        }
    }

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        unmockkAll()
    }

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        withEnvironment(envVariables, OverrideMode.SetOrOverride) {
            val reportFolder = uploadReportsToArtifactory(
                artifactoryClient, repoName,
                fortifyName = fortifyName,
                owaspName = owaspName,
                zapName = zapName,
                ccaName = ccaName,
                //genericReportName = genericReportName,
            )
            fortifyName = reportFolder.fortifyName
            owaspName = reportFolder.owaspName
            zapName = reportFolder.zapName
            ccaName = reportFolder.ccaName
            //genericReportName = reportFolder.genericReportName
        }
        // Mocking Version validation due to issues of CosmosDB that occur when using wihtConstantNow
        mockCosmosDBVersionInfo()
        mockCosmosDBClient()
    }
}
