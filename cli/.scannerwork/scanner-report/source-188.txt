package de.deutschepost.sdm.cdlib.release

import de.deutschepost.sdm.cdlib.CdlibCommand
import de.deutschepost.sdm.cdlib.change.metrics.model.CdlibVersionConfig
import de.deutschepost.sdm.cdlib.release.report.external.cca.CSS_QHCR_HARBOR
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import toArgsArray
import withStandardOutput

@RequiresTag("UnitTest")
@Tags("UnitTest")
@MicronautTest
class ReportFetchCssCommandTest(private val cdlibVersionConfig: CdlibVersionConfig) : FunSpec() {
    init {
        test("report fetch -h should display help") {
            val args = "report fetch css -h".toArgsArray()
            val (_, output) = withStandardOutput { PicocliRunner.run(CdlibCommand::class.java, *args) }
            output shouldContain "Fetches your CCA reports from QHCR Harbor."
        }

        context("Fail on other container registry") {
            val args =
                "--debug --image docker.artifactory.dhl.com/cdlib/cdlib-cli:20220609.1146.13-master --robot-account foo --token bar".toArgsArray()
            val (ret, output) = withStandardOutput {
                PicocliRunner.call(ReportCommand.FetchCommand.CSSCommand::class.java, *args)
            }
            test("Display invalid container registry") {
                ret shouldBe -1
                output shouldContain "Invalid container registry docker.artifactory.dhl.com"
            }
        }

        test("Fail on malformed image string") {
            val args =
                "--debug --image $CSS_QHCR_HARBOR/cdlib/cdlib-cli --robot-account foo --token bar".toArgsArray()
            val (ret, output) = withStandardOutput {
                PicocliRunner.call(ReportCommand.FetchCommand.CSSCommand::class.java, *args)
            }
            ret shouldBe -1
            output shouldContain "Failed to parse"
        }
    }
}
