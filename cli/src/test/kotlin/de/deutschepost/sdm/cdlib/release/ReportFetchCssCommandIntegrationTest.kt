package de.deutschepost.sdm.cdlib.release

import de.deutschepost.sdm.cdlib.release.report.TestResultPrefixes
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.SystemEnvironmentTestListener
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
class ReportFetchCssCommandIntegrationTest(
    @Value("\${harbor.username}") val username: String,
    @Value("\${harbor.token}") val token: String,
) : StringSpec() {
    private val releaseName = "integration-test-1337"
    private val reportFileName = "${TestResultPrefixes.DEFAULT_PREFIX_CCA}-trivy-$releaseName.json"
    private val image = "dpdhl.css-qhcr-pi.azure.deutschepost.de/cdlib-integration/postgres:10.19-alpine"

    override fun listeners() = listOf(
        SystemEnvironmentTestListener(
            mapOf(
                "CDLIB_RELEASE_NAME" to releaseName,
                "CDLIB_APP_NAME" to "cli",
                "CDLIB_PM_GIT_MAIL" to "integration-test-git-mail",
                "CDLIB_PM_GIT_NAME" to "integration-test-git-author",
                "CDLIB_PM_GIT_ID" to "integration-test-git-id",
                "CDLIB_PM_GIT_LINK" to "integration-test-git-link",
                "CDLIB_PM_GIT_MESSAGE" to "integration-test-git-message",
                "CDLIB_PM_GIT_ORIGIN" to "integration-test-git-origin",
                "CDLIB_CICD_PLATFORM" to "integration-test-platform",
                "CDLIB_JOB_URL" to "integration-test-platform.com/integration-test",
            ), OverrideMode.SetOrOverride
        )
    )

    init {
        "Download report and check it" {
            val reportFile = File(reportFileName)

            reportFile.exists() shouldBe false
            val fetchArgs =
                "--debug --image $image --robot-account $username --token $token".toArgsArray()
            val (fetchRet, fetchOut) = withStandardOutput {
                PicocliRunner.call(ReportCommand.FetchCommand.CSSCommand::class.java, *fetchArgs)
            }

            fetchRet shouldBeExactly 0
            reportFile.exists() shouldBe true
            fetchOut shouldContain "Harbor CCA scan completed"
            fetchOut shouldContain "Fetching CCA report"
            fetchOut shouldContain "Created Trivy CCA report"
            fetchOut shouldContain "Writing Trivy CCA report"

            val checkArgs =
                "--debug --severity CRITICAL -f $reportFileName".toArgsArray()
            val (checkRet, checkOut) = withStandardOutput {
                PicocliRunner.call(ReportCommand.CheckCommand::class.java, *checkArgs)
            }

            checkRet shouldBe -1
            checkOut shouldContain "[CCA] has relevant vulnerabilities at"
        }
        "Useful error message when scanning a helm chart" {
            val args =
                "--debug --image dpdhl.css-qhcr-pi.azure.deutschepost.de/cdlib-integration/headlamp-k8s.github.io/headlamp/headlamp:0.26.0 --robot-account $username --token $token".toArgsArray()
            val (ret, output) = withStandardOutput {
                PicocliRunner.call(ReportCommand.FetchCommand.CSSCommand::class.java, *args)
            }
            ret shouldBeExactly -1
            output shouldContain "Only images are supported! You tried to scan a CHART"
        }

        "Useful error message when image is not present" {
            val args =
                "--debug --image dpdhl.css-qhcr-pi.azure.deutschepost.de/sdm/nodebrady:not-preset --robot-account $username --token $token".toArgsArray()
            val (ret, output) = withStandardOutput {
                PicocliRunner.call(ReportCommand.FetchCommand.CSSCommand::class.java, *args)
            }
            ret shouldBeExactly -1
            output shouldContain "Failed to find image in Harbor."
        }
    }

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        val file = File(reportFileName)
        if (file.exists()) {
            file.delete()
        }
    }
}
