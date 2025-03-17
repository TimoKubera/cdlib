package de.deutschepost.sdm.cdlib.release

import de.deutschepost.sdm.cdlib.CdlibCommand
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.micronaut.configuration.picocli.PicocliRunner
import toArgsArray
import withStandardOutput

@RequiresTag("UnitTest")
@Tags("UnitTest")
class ReportUploadCommandTest : FunSpec({
    context("Basic test") {
        test("Should be able to run.") {
            val (_, output) = withStandardOutput {
                val args = "report upload -h".toArgsArray()
                PicocliRunner.run(CdlibCommand::class.java, *args)
            }
            output shouldContain "Analyzes and uploads your"
        }
    }
})
