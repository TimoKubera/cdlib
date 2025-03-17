package de.deutschepost.sdm.cdlib.release.report


import de.deutschepost.sdm.cdlib.release.report.external.OdcTestResult
import de.deutschepost.sdm.cdlib.release.report.external.from
import de.deutschepost.sdm.cdlib.release.report.internal.SecurityTestResult
import de.deutschepost.sdm.cdlib.utils.permissiveObjectMapper
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.file.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File

inline fun <reified T> parseJSON(file: File): T {
    return permissiveObjectMapper.treeToValue(permissiveObjectMapper.readTree(file.inputStream()), T::class.java)
}

@RequiresTag("UnitTest")
@Tags("UnitTest")
class ReportTest : FunSpec({
    context("General functionality") {
        test("Jackson should be able to serialize OdcReport.") {
            val testResult = SecurityTestResult.from(
                parseJSON<OdcTestResult>(File("src/test/resources/odc/odc-carts-short.json")),
                "file://src/test/resources/odc/odc-carts-short.json"
            )
            testResult.toJsonNode().shouldNotBeNull()
            print(testResult.toPrettyString())
        }

        test("Jackson should be able to write JSON to file.") {
            val testResult = SecurityTestResult.from(
                parseJSON<OdcTestResult>(File("src/test/resources/odc/odc-carts-short.json")),
                "file://src/test/resources/odc/odc-carts-short.json"
            )
            val file = tempfile("test", ".json")
            testResult.writeJson(file.absolutePath)
            file.shouldNotBeEmpty()
            print(file.readText())
        }

        test("Jackson should be able to serialize ZapReport.") {
            val testResults = TestResultParser.parse(
                File("src/test/resources/zap/zap-report_with_open.json"),
            )
            testResults?.toJsonNode().shouldNotBeNull()
            print(testResults?.toPrettyString())
        }

        test("Jackson should be able to serialize FortifyReport.") {
            val testResults = TestResultParser.parse(File("src/test/resources/fortify/fortify.fpr"))
            testResults.shouldNotBeNull()
            testResults.toJsonNode().shouldNotBeNull()
            testResults.shouldBeInstanceOf<SecurityTestResult>()
            testResults.pregenerated shouldBe false
            print(testResults.toPrettyString())
        }

        test("Jackson should be able to serialize GenericReport.") {
            val testResults =
                TestResultParser.parse(
                    file = File("src/test/resources/generic.json"),
                    substitutes = listOf("generic.json")
                )
            testResults.shouldNotBeNull()
            testResults.shouldBeInstanceOf<SecurityTestResult>()
            testResults.pregenerated shouldBe true
            println(testResults.toPrettyString())
        }

        test("Jackson should be able to serialize CcaReport.") {
            val testResults = TestResultParser.parse(file = File("src/test/resources/trivy/cca-trivy.json"))
            testResults.shouldNotBeNull()
            testResults.shouldBeInstanceOf<SecurityTestResult>()
            testResults.pregenerated shouldBe true
            println(testResults.toPrettyString())
        }
    }
})
