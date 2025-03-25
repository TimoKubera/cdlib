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
class ReportOslcGradlePluginIntegrationTest(
    @Value("\${artifactory-its-identity-token}") val artifactoryIdentityToken: String,
) : FunSpec() {

    private val appName = "cli"
    private val releaseName = "Integration_result_0228"
    private val jsonFile = "src/test/resources/passing/${TestResultPrefixes.DEFAULT_PREFIX_OSLC_GRADLE_PLUGIN}.json"
    private val jsonFailingFile =
        "src/test/resources/oslc/${TestResultPrefixes.DEFAULT_PREFIX_OSLC_GRADLE_PLUGIN}_failing.json"
    private val repoName = "sdm-proj-prg-cdlib-cli-appimage"
    private val acceptedListFile = "src/test/resources/oslc/acceptedList.json"
    private val acceptedListFileWrongLicense = "src/test/resources/oslc/acceptedList-wrongLicense.json"
    private val acceptedListFileWrongVersion = "src/test/resources/oslc/acceptedList-wrongVersion.json"

    override fun listeners() = listOf(
        getSystemEnvironmentTestListenerWithOverrides(
            mapOf(
                "CDLIB_APP_NAME" to appName,
                "CDLIB_RELEASE_NAME" to releaseName
            )
        )
    )

    init {
        companion object {
            const val POLICY_PROFILE_DISTRIBUTION = "Policy Profile: Distribution"
        }
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
                output shouldContain "Uploaded Artifact:"
            }
    
            test("Check OSLC-Plugin report locally should fail due to distribution flag") {
                val args = "--debug --files $jsonFile --distribution".toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                }
                ret shouldBeExactly -1
                output shouldContain POLICY_PROFILE_DISTRIBUTION
                output shouldNotContain "Unapproved Licenses Count: 0"
            }
    
            test("Check OSLC-Plugin report locally should succeed with accepted list") {
                val args =
                    "--debug --files $jsonFile --distribution --oslc-accepted-list $acceptedListFile".toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                }
                ret shouldBeExactly 0
                output shouldContain POLICY_PROFILE_DISTRIBUTION
                output shouldContain "Unapproved Licenses Count: 0"
            }
    
            test("Check OSLC-Plugin report locally should fail with accepted list because of wrong license") {
                val args =
                    "--debug --files $jsonFile --distribution --oslc-accepted-list $acceptedListFileWrongLicense".toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                }
                ret shouldBeExactly -1
                output shouldContain POLICY_PROFILE_DISTRIBUTION
                output shouldContain "Unapproved Licenses Count: 1"
                output shouldContain "Alladin Free Public License 9: [com.rabbitmq:amqp-client]"
            }
    
            test("Check OSLC-Plugin report locally should fail with accepted list because of wrong version number") {
                val args =
                    "--debug --files $jsonFile --distribution --oslc-accepted-list $acceptedListFileWrongVersion".toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                }
                ret shouldBeExactly -1
                output shouldContain POLICY_PROFILE_DISTRIBUTION
                output shouldContain "Unapproved Licenses Count: 1"
                output shouldContain "Common Development and Distribution License 1.0: [org.apache.tomcat.embed:tomcat-embed-core]"
            }
    
    

            test("Trying to upload failing OSLC-Plugin report to artifactory should missing") {

                val args =
                    "--debug --files $jsonFailingFile --artifactory-its-instance --artifactory-identity-token $artifactoryIdentityToken --repo-name $repoName --type build".toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(ReportCommand.UploadCommand::class.java, *args)
                }
                ret shouldBeExactly -1
                output shouldNotContain "Uploaded Artifact:"
            }
        }
    }

}
