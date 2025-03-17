package de.deutschepost.sdm.cdlib

import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain
import io.micronaut.configuration.picocli.PicocliRunner
import withStandardOutput

@RequiresTag("UnitTest")
@Tags("UnitTest")
class CdlibCommandSpec : BehaviorSpec({


    given("cdlib") {
        `when`("invocation") {
            val (_, output) = withStandardOutput {
                val args = emptyArray<String>()
                PicocliRunner.run(CdlibCommand::class.java, *args)
            }
            then("should display version") {
                output shouldContain "Starting CDlib 0.0.0-INTEGRATION-TEST"
            }
        }
    }
})
