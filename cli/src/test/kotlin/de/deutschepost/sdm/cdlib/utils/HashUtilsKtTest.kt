package de.deutschepost.sdm.cdlib.utils


import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import java.io.File

/**
 * ATTENTION: Requires zap.json and dependency-check-report.json to have LF line separators (CRLF will cause hash mismatch)
 */
@RequiresTag("UnitTest")
@Tags("UnitTest")
class HashUtilsKtTest : FunSpec({

    data class HashComparisonPair(val file: File, val expectedHash: String)

    val prefix = "src/test/resources/passing"
    val files = listOf(
        HashComparisonPair(
            File("$prefix/zap.json"),
            "72ba3b4a28ae638709c3d1cf04cf4ad22202feed932cc970e7b9756a5b1f6b08"
        ),
        HashComparisonPair(
            File("$prefix/fortify.fpr"),
            "6897b2fb3676b1a05c6c09a5cabfa751c82ccc7c89fa09cc7fef92aae769aa1d"
        ),
        HashComparisonPair(
            File("$prefix/dependency-check-report.json"),
            "da23f0e6082cc7b53cc5b7967c9d688f7c0f4ed4fd0c2067ac82aa1847ec1015"
        )
    )

    files.forEach { hashComparisonPair ->
        test("${hashComparisonPair.file} has expected sum") {
            hashComparisonPair.file.sha256sum() shouldBeEqualComparingTo hashComparisonPair.expectedHash
        }
    }
})
