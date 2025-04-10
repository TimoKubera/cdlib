package de.deutschepost.sdm.cdlib.change

import de.deutschepost.sdm.cdlib.artifactory.ArtifactoryClient
import de.deutschepost.sdm.cdlib.artifactory.ITS_ARTIFACTORY_URL
import de.deutschepost.sdm.cdlib.utils.mockCosmosDBClient
import de.deutschepost.sdm.cdlib.utils.mockCosmosDBVersionInfo
import de.deutschepost.sdm.cdlib.utils.uploadReportsToArtifactory
import getSystemEnvironmentTestListenerWithOverrides
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.withEnvironment
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.unmockkAll
import mu.KLogging
import toArgsArray
import withStandardOutput
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.random.Random

@RequiresTag("IntegrationTest")
@Tags("IntegrationTest")
@MicronautTest
class ChangeOslcIntegrationTest(
    @Value("\${artifactory-its-identity-token}") val artifactoryIdentityToken: String,
    @Value("\${change-management-token}") val chgToken: String,
) : FunSpec() {

    private val timestamp =
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))

    private val appName = "cli"
    private val appVersion = Random.nextInt(1337, 31337)
    private val releaseName = "${appName}_${appVersion}"
    private val releaseNameUnique = "${releaseName}_$timestamp"

    private val artifactoryClient = ArtifactoryClient(artifactoryIdentityToken, ITS_ARTIFACTORY_URL)
    private val repoName = "sdm-proj-prg-cdlib-cli-appimage"
    private val immutableRepoName = "sdm-proj-prg-cdlib-cli-appimage"

    private var oslcFNCIName = "Integration-oslc-fnci-$timestamp"
    private var oslcMavenPluginName = "Integration-oslc-maven-plugin-$timestamp"
    private var oslcGradlePluginName = "Integration-oslc-gradle-plugin-$timestamp"

    private var genericReportName = "Integration-generic-$timestamp"

    private val envVariables = mutableMapOf(
        "CDLIB_RELEASE_NAME_UNIQUE" to releaseNameUnique,
        "CDLIB_RELEASE_NAME" to releaseName,
        "CDLIB_APP_NAME" to appName,
        "CDLIB_APP_VERSION" to timestamp,
        "CDLIB_PM_GIT_MAIL" to "integration-test-git-mail",
        "CDLIB_PM_GIT_NAME" to "integration-test-git-author",
        "CDLIB_PM_GIT_ID" to "integration-test-git-id",
        "CDLIB_PM_GIT_LINK" to "integration-test-git-link",
        "CDLIB_PM_GIT_MESSAGE" to "integration-test-git-message",
        "CDLIB_PM_GIT_ORIGIN" to "integration-test-git-origin",
        "CDLIB_CICD_PLATFORM" to "integration-test-platform",
        "CDLIB_JOB_URL" to "integration-test-platform.com/integration-test",
    )

    override fun listeners() = listOf(
        getSystemEnvironmentTestListenerWithOverrides(envVariables)
    )

    override suspend fun beforeTest(testCase: io.kotest.core.test.TestCase) {
        super.beforeTest(testCase)
        envVariables["CDLIB_RELEASE_NAME_UNIQUE"] = "${releaseNameUnique}_${testCase.name.testName.replace(" ", "_")}"
    }

    init {
        context("Creating oslc change is a success") {
            test("change create --oslc missing distribution") {
                val (ret, output) = withStandardOutput {
                    val args =
                        "--jira-token $chgToken --artifactory-its-instance --artifactory-identity-token $artifactoryIdentityToken --repo-name $repoName --immutable-repo-name $immutableRepoName --folder-name $oslcFNCIName --commercial-reference 5296 --test --debug".toArgsArray()
                    PicocliRunner.call(ChangeCommand.CreateCommand::class.java, *args)
                }
                ret shouldBe -1
                output shouldContain "IllegalArgumentException"
            }

            test("change create with distribution") {
                val (ret, output) = withStandardOutput {
                    val args =
                        "--no-distribution --jira-token $chgToken --artifactory-its-instance --artifactory-identity-token $artifactoryIdentityToken --repo-name $repoName --immutable-repo-name $immutableRepoName --folder-name $oslcFNCIName --commercial-reference 5296 --test --debug --no-webapproval --no-tqs".toArgsArray()
                    PicocliRunner.call(ChangeCommand.CreateCommand::class.java, *args)
                }
                output shouldContain ENTRY_URL
                output shouldContain "labels=[cdlib, oslc, test]"
                ret shouldBeExactly 0
            }

            test("change create with report from maven plugin") {
                val (ret, output) = withStandardOutput {
                    val args =
                        "--no-distribution --jira-token $chgToken --artifactory-its-instance --artifactory-identity-token $artifactoryIdentityToken --repo-name $repoName --immutable-repo-name $immutableRepoName --folder-name $oslcMavenPluginName --commercial-reference 5296 --test --debug --no-webapproval --no-tqs".toArgsArray()
                    PicocliRunner.call(ChangeCommand.CreateCommand::class.java, *args)
                }
                output shouldContain ENTRY_URL
                output shouldContain "labels=[cdlib, oslc, test]"
                output shouldNotContain "com.microsoft.aad.msal4j.MsalClientException: Token not found in the cache"
                ret shouldBeExactly 0
            }

            test("change create with report from gradle plugin") {
                val (ret, output) = withStandardOutput {
                    val args =
                        "--no-distribution --jira-token $chgToken --artifactory-its-instance --artifactory-identity-token $artifactoryIdentityToken --repo-name $repoName --immutable-repo-name $immutableRepoName --folder-name $oslcGradlePluginName --commercial-reference 5296 --test --debug --no-webapproval --no-tqs".toArgsArray()
                    PicocliRunner.call(ChangeCommand.CreateCommand::class.java, *args)
                }
                output shouldContain ENTRY_URL
                output shouldContain "labels=[cdlib, oslc, test]"
                output shouldNotContain "com.microsoft.aad.msal4j.MsalClientException: Token not found in the cache"
                ret shouldBeExactly 0
            }
        }

        withEnvironment(envVariables + mapOf("CDLIB_APP_NAME" to "appName_TEST"), OverrideMode.SetOrOverride) {
            val reportFolder = uploadReportsToArtifactory(
                artifactoryClient, repoName,
                genericReportName = genericReportName
            )
            genericReportName = reportFolder.genericReportName
        }

        context("Creating oslc change is a failure") {
            test("Missing Reports in oslc verification") {
                val (ret, output) = withStandardOutput {
                    val args =
                        "--no-distribution --jira-token $chgToken --artifactory-its-instance --artifactory-identity-token $artifactoryIdentityToken --repo-name $repoName --immutable-repo-name $immutableRepoName --folder-name $oslcFNCIName --folder-name $genericReportName --commercial-reference 5296 --test --debug --no-webapproval --no-tqs".toArgsArray()
                    PicocliRunner.call(ChangeCommand.CreateCommand::class.java, *args)
                }

                output shouldContain "App appName_TEST is missing OSLC report!"
                output shouldContain "Missing OSLC reports!"
                ret shouldBeExactly -1
            }
        }
    }

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        withEnvironment(envVariables, OverrideMode.SetOrOverride) {
            val reportFolder = uploadReportsToArtifactory(
                artifactoryClient, repoName,
                oslcFNCIName = oslcFNCIName,
                oslcMavenPluginName = oslcMavenPluginName,
                oslcGradlePluginName = oslcGradlePluginName,
            )
            oslcFNCIName = reportFolder.oslcFNCIName
            oslcMavenPluginName = reportFolder.oslcMavenPluginName
            oslcGradlePluginName = reportFolder.oslcGradlePluginName
        }
        mockCosmosDBVersionInfo()
        mockCosmosDBClient()
    }

    companion object : KLogging()

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        unmockkAll()
    }

}
