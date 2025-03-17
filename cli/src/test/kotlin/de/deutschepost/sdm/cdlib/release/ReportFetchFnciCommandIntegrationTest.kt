package de.deutschepost.sdm.cdlib.release


import de.deutschepost.sdm.cdlib.archive.ArchiveCommand.UploadCommand
import de.deutschepost.sdm.cdlib.release.report.TestResultPrefixes
import getSystemEnvironmentTestListenerWithOverrides
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import toArgsArray
import withStandardOutput
import java.io.File

@RequiresTag("IntegrationTest")
@Tags("IntegrationTest")
@MicronautTest
class ReportFetchFnciCommandIntegrationTest(
    @Value("\${fnci-token}") val token: String,
    @Value("\${artifactory-its-identity-token}") val artifactoryIdentityToken: String,
) : FunSpec() {

    private val releaseName = "Integration_result_0228"
    private val reportFilesBasename = "${TestResultPrefixes.DEFAULT_PREFIX_FNCI}-$releaseName"
    private val projectId = 274
    private val repoName = "sdm-proj-prg-cdlib-cli-appimage"

    override fun listeners() = listOf(
        getSystemEnvironmentTestListenerWithOverrides(
            mapOf(
                "CDLIB_RELEASE_NAME" to releaseName
            )
        )
    )

    init {
        context("Generate/fetch FNCI report and upload") {
            val reportFile = File("$reportFilesBasename.zip")
            val jsonFile = File("$reportFilesBasename.json")
            test("Download report") {

                reportFile.exists() shouldBe false

                val fetchArgs =
                    "--debug --project-id $projectId --fnci-token $token".toArgsArray()
                val (fetchRet, fetchOut) = withStandardOutput {
                    PicocliRunner.call(ReportCommand.FetchCommand.FnciCommand::class.java, *fetchArgs)
                }

                fetchOut shouldContain "Starting generating a report"
                fetchOut shouldContain "Successfully generated taskID:"
                fetchOut shouldContain "Client will try to fetch report"
                fetchOut shouldContain "Successfully fetched FNCI report to"

                fetchRet shouldBeExactly 0


                reportFile.exists() shouldBe true
                reportFile.deleteOnExit()

                jsonFile.exists() shouldBe true
                jsonFile.deleteOnExit()
            }

            test("Upload FNCI report to artifactory") {
                val args =
                    "--debug --files $reportFile --artifactory-its-instance --artifactory-identity-token $artifactoryIdentityToken --repo-name $repoName --type build".toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(UploadCommand::class.java, *args)
                }
                ret shouldBeExactly 0
                output shouldContain "Uploaded Artifact:"
            }

            test("Check FNCI report locally") {
                val args =
                    "--debug --files $jsonFile --no-distribution".toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                }
                ret shouldBeExactly 0
                output shouldContain "Policy Profile: Non-Distribution"
            }
        }
    }

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        for (file in listOf(
            File("$reportFilesBasename.zip"),
            File("$reportFilesBasename.json")
        )
        ) {
            if (file.exists()) {
                file.delete()
            }
        }
    }

}
