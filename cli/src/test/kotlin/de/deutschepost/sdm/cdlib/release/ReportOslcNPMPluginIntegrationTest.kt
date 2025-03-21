package de.deutschepost.sdm.cdlib.release

import de.deutschepost.sdm.cdlib.release.report.TestResultPrefixes
import getSystemEnvironmentTestListenerWithOverrides
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import toArgsArray
import withStandardOutput


@RequiresTag("IntegrationTest")
@Tags("IntegrationTest")
@MicronautTest
class ReportOslcNPMPluginIntegrationTest(
    @Value("\${artifactory-its-identity-token}") val artifactoryIdentityToken: String,
    @Value("\${artifactory-azure-identity-token}") val artifactoryLCMIdentityToken: String,
) : FunSpec() {

    private val appName = "cli"
    private val releaseName = "Integration_result_0228"
    private val jsonFile = "src/test/resources/passing/${TestResultPrefixes.DEFAULT_PREFIX_OSLC_NPM_PLUGIN}.json"
    private val jsonFailingFile =
        "src/test/resources/oslc/${TestResultPrefixes.DEFAULT_PREFIX_OSLC_NPM_PLUGIN}_failing.json"
    private val repoName = "sdm-proj-prg-cdlib-cli-appimage"
    private val repoLCMName = "ICTO-3339_sdm_sockshop_release_reports"

    override fun listeners() = listOf(
        getSystemEnvironmentTestListenerWithOverrides(
            mapOf(
                "CDLIB_APP_NAME" to appName,
                "CDLIB_RELEASE_NAME" to releaseName
            )
        )
    )

    init {
        private val UPLOADED_ARTIFACT_MSG = "Uploaded Artifact:"
    
        context("Check OSLC-Plugin report and upload") {
            test("Check OSLC-Plugin report locally") {
                val args = "--debug --files $jsonFile --no-distribution".toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                }
                ret shouldBeExactly 0
                output shouldContain "Policy Profile: Non-Distribution"
            }
    
            test("Upload OSLC-Plugin report to artifactory") {
                val args =
                    "--debug --files $jsonFile --no-distribution --artifactory-its-instance --artifactory-identity-token $artifactoryIdentityToken --repo-name $repoName --type build".toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(ReportCommand.UploadCommand::class.java, *args)
                }
                ret shouldBeExactly 0
                output shouldContain UPLOADED_ARTIFACT_MSG
            }
            // TODO: Delete after sundown
            test("Upload OSLC-Plugin report to LCM artifactory") {
                val args =
                    "--debug --files $jsonFile --no-distribution --artifactory-azure-instance --artifactory-identity-token $artifactoryLCMIdentityToken --repo-name $repoLCMName --type build".toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(ReportCommand.UploadCommand::class.java, *args)
                }
                ret shouldBeExactly 0
                output shouldContain UPLOADED_ARTIFACT_MSG
            }

            test("Check OSLC-Plugin report locally should fail due to distribution flag") {
                val args = "--debug --files $jsonFailingFile --distribution".toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                }
                ret shouldBeExactly -1
                output shouldContain "Policy Profile: Distribution"
            }

            test("Trying to upload failing OSLC-Plugin report to artifactory should missing") {

                val args =
                    "--debug --files $jsonFailingFile --distribution --artifactory-its-instance --artifactory-identity-token $artifactoryIdentityToken --repo-name $repoName --type build".toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(ReportCommand.UploadCommand::class.java, *args)
                }
                ret shouldBeExactly -1
                output shouldNotContain "Uploaded Artifact:"
            }
        }
    }

}
