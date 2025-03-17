package de.deutschepost.sdm.cdlib.release.report

import com.fasterxml.jackson.core.JsonParseException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import withStandardOutput

@RequiresTag("UnitTest")
@Tags("UnitTest")
class TestResultParserTest : FunSpec({
    test("Malformed JSON should cause an exception.") {
        val (_, output) = withStandardOutput {
            val f = tempfile(prefix = "zap", suffix = ".json")
            f.writeText("JSON Statham")
            shouldThrow<JsonParseException> {
                TestResultParser.parse(f)
            }
        }
        output shouldContain "seems to be malformed."
    }

    test("Report Parser should parse appropriate types.") {
        val zap = tempfile("zap", suffix = ".html")
        val odc = tempfile("dependency-check", suffix = ".html")
        val (_, output) = withStandardOutput {
            TestResultParser.parse(zap) shouldBe null
            TestResultParser.parse(odc) shouldBe null
        }
        output shouldContain "Skipping file: ${zap.name}"
        output shouldContain "Skipping file: ${odc.name}"
    }

})
