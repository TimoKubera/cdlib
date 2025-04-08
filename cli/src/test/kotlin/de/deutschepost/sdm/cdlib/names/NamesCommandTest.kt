package de.deutschepost.sdm.cdlib.names

import de.deutschepost.sdm.cdlib.CdlibCommand
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.string.shouldContain
import io.micronaut.configuration.picocli.PicocliRunner
import toArgsArray
import withStandardOutput

@RequiresTag("UnitTest")
@Tags("UnitTest")
package de.deutschepost.sdm.cdlib.names

import de.deutschepost.sdm.cdlib.CdlibCommand
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.string.shouldContain
import io.micronaut.configuration.picocli.PicocliRunner
import toArgsArray
import withStandardOutput

@RequiresTag("UnitTest")
@Tags("UnitTest")
class NamesCommandTest : DescribeSpec({
    describe("cdlib names") {
        describe("names -h") {
            val (_, output) = withStandardOutput {
                val args = "names -h".toArgsArray()
                PicocliRunner.run(CdlibCommand::class.java, *args)
            }
            it(NamesCommandTest.HELP_DISPLAY) {
                output shouldContain "Contains subcommands for automatic name and ID creation in pipeline"
            }
        }

        describe("names create -h") {
            val (_, output) = withStandardOutput {
                val args = "names create -h".toArgsArray()
                PicocliRunner.run(CdlibCommand::class.java, *args)
            }

            it(NamesCommandTest.HELP_DISPLAY) {
                output shouldContain "Create canonical set of standard names and IDs"
                output shouldContain "Name of optional output file"
                output shouldContain "Release name to use for derived name creation"
                output shouldContain "Sample: frontend_20211203.1827.54_25_a5c5bc3"
            }
        }

        describe("names dump -h") {
            val (_, output) = withStandardOutput {
                val args = "names dump -h".toArgsArray()
                PicocliRunner.run(CdlibCommand::class.java, *args)
            }

            it(NamesCommandTest.HELP_DISPLAY) {
                output shouldContain "Dumps the list of environment variables"
                output shouldContain "Name of optional output file"
            }
        }

        describe("names dump") {
            val (_, output) = withStandardOutput {
                val args = "names dump".toArgsArray()
                PicocliRunner.run(CdlibCommand::class.java, *args)
            }

            it("should display env. vars") {
                output shouldContain "Current working directory"
                output shouldContain "Detected runtime platform"
            }
        }
    }
}) {
    companion object {
        const val HELP_DISPLAY = "should display help"
    }
}
