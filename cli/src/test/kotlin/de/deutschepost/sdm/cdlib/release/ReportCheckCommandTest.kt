package de.deutschepost.sdm.cdlib.release

import de.deutschepost.sdm.cdlib.CdlibCommand
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.SystemEnvironmentTestListener
import io.kotest.extensions.time.withConstantNow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micronaut.configuration.picocli.PicocliRunner
import toArgsArray
import withStandardOutput
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@RequiresTag("UnitTest")
@Tags("UnitTest")
class ReportCheckCommandTest : FunSpec() {
    private val fixedDateTimeOct21 = LocalDate.of(2021, 10, 31).atStartOfDay(ZoneId.systemDefault())
    private val zonedDateTimeMax = LocalDateTime.MAX.atZone(ZoneId.systemDefault())
    private val beginningLastCentury = LocalDate.of(1900, 1, 1).atStartOfDay(ZoneId.systemDefault())

    override fun listeners(): List<TestListener> = listOf(
        SystemEnvironmentTestListener(
            mapOf(
                "CDLIB_APP_NAME" to "cli",
                "CDLIB_PM_GIT_MAIL" to "integration-test-git-mail",
                "CDLIB_PM_GIT_NAME" to "integration-test-git-author",
                "CDLIB_PM_GIT_ID" to "integration-test-git-id",
                "CDLIB_PM_GIT_LINK" to "integration-test-git-link",
                "CDLIB_PM_GIT_MESSAGE" to "integration-test-git-message",
                "CDLIB_PM_GIT_ORIGIN" to "integration-test-git-origin",
                "CDLIB_CICD_PLATFORM" to "integration-test-platform",
                "CDLIB_JOB_URL" to "https://integration-test-url.jenkuns.example.com/foo/bar/job/1337",
            ), OverrideMode.SetOrOverride
        )
    )


    init {

        test("report check -h should display help") {
            val args = "report check -h".toArgsArray()
            val (_, output) = withStandardOutput {
                PicocliRunner.run(CdlibCommand::class.java, *args)
            }

            output shouldContain "Checks your reports for known issues."
        }

        test("report check help includes deprecated note") {
            val args = "report check -h".toArgsArray()
            val (_, output) = withStandardOutput {
                 PicocliRunner.run(CdlibCommand::class.java, *args)
            }
            output shouldContain "Deprecated"
        }
        test("Don't fail up-to-date reports.") {
            withConstantNow(beginningLastCentury) {
                val args =
                    "--debug --report-prefix-zap _zap --files src/test/resources/zap/_zap-report_with_open_outdated.json".toArgsArray()
                val ret = PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                ret shouldBe 0
            }
        }

        test("Fail ZAP for LOW severity.") {
            withConstantNow(fixedDateTimeOct21) {
                val args =
                    "--severity LOW --files src/test/resources/zap/zap-report_with_open.json --files src/test/resources/zap/zap-report_all_suppressed.json".toArgsArray()
                val ret = PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                ret shouldBe -1
            }
        }

        test("Don't fail ZAP for HIGH severity.") {
            withConstantNow(fixedDateTimeOct21) {
                val args =
                    "--debug --files src/test/resources/zap/zap-report_with_open.json --files src/test/resources/zap/zap-report_all_suppressed.json".toArgsArray()
                val ret = PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                ret shouldBe 0
            }
        }

        test("Fail ODC for normal check.") {
            withConstantNow(fixedDateTimeOct21) {
                val args =
                    "--debug --files src/test/resources/odc/odc-carts-short.json --report-prefix-odc odc".toArgsArray()
                val ret = PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                ret shouldBe -1
            }
        }

        test("Fail OSLC for license 'I don't know'.") {
            withConstantNow(fixedDateTimeOct21) {
                val args =
                    "--debug --files src/test/resources/fnci/oslc-fnci-report_failing.json".toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                }
                ret shouldBe -1
                output shouldContain "Unapproved Licenses Count: 1"
                output shouldContain " - I don't know: "
            }
        }

        test("Fail report check with ANT pattern and LOW severity.") {
            withConstantNow(fixedDateTimeOct21) {
                val args =
                    "--debug --severity LOW --files src${File.separatorChar}test${File.separatorChar}resources${File.separatorChar}**${File.separatorChar}*.json".toArgsArray()
                val ret = PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                ret shouldBe -1
            }
        }

        test("Fail check with other prefix.") {
            withConstantNow(fixedDateTimeOct21) {
                val args =
                    "--debug --report-prefix-odc _odc --files src/test/resources/odc/_odc-carts.json".toArgsArray()
                val ret = PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                ret shouldBe -1
            }
        }

        test("Fail fortify for default file.") {
            withConstantNow(beginningLastCentury) {
                val args =
                    "--debug --files src/test/resources/fortify/fortify.fpr".toArgsArray()
                val ret = PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                ret shouldBe -1
            }
        }

        test("Don't fail fortify for passing file.") {
            withConstantNow(beginningLastCentury) {
                val args =
                    "--debug --files src/test/resources/passing/fortify.fpr".toArgsArray()
                val ret = PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                ret shouldBe 0
            }
        }

        test("Fail outdated check.") {
            withConstantNow(zonedDateTimeMax) {
                val args =
                    "--debug --report-prefix-zap _zap --files src/test/resources/zap/_zap-report_with_open_outdated.json".toArgsArray()
                val ret = PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                ret shouldBe -1
            }
        }

        test("Fail if no report is found.") {
            withConstantNow(zonedDateTimeMax) {
                val args =
                    "--debug --report-prefix-zap _zap --files src/test/resources/passing/zap.json".toArgsArray()
                val ret = PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                ret shouldBe -1
            }
        }

        test("Fail if no files are found.") {
            withConstantNow(zonedDateTimeMax) {
                val args =
                    "--debug --report-prefix-zap _zap --files does/not/exist/*".toArgsArray()
                val ret = PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                ret shouldBe -1
            }
        }

        test("Fail for generic.") {
            withConstantNow(zonedDateTimeMax) {
                val args =
                    "--debug -g src/test/resources/generic.json".toArgsArray()
                val ret = PicocliRunner.call(ReportCommand.CheckCommand::class.java, *args)
                ret shouldBe -1
            }
        }
    }
}
