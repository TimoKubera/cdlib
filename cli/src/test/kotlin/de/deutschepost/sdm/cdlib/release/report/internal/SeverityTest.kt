package de.deutschepost.sdm.cdlib.release.report.internal

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

@RequiresTag("UnitTest")
@Tags("UnitTest")
class SeverityTest : FunSpec({
    listOf("UNKNOWN", "NONE", "LOW", "MEDIUM", "HIGH", "CRITICAL").forEach {
        test("resilientValueOf should have the same value as valueOf - $it") {
            Severity.resilientValueOf(it) shouldBe Severity.valueOf(it)
        }
    }



    test("resilientValueOf should be able to translate lowercase name") {
        Severity.resilientValueOf("low") shouldBe Severity.LOW
        Severity.resilientValueOf("medium") shouldBe Severity.MEDIUM
    }

    test("resilientValueOf should be able to translate known other names") {
        Severity.resilientValueOf("moderate") shouldBe Severity.MEDIUM
    }

    test("resilientValueOf should throw an IllegalArgumentException for unknown names") {
        shouldThrow<IllegalArgumentException> {
            Severity.resilientValueOf("pancakes")
        }
    }
})
