package de.deutschepost.sdm.cdlib.release.report

import de.deutschepost.sdm.cdlib.release.report.external.OdcTestResult
import de.deutschepost.sdm.cdlib.release.report.external.from
import de.deutschepost.sdm.cdlib.release.report.internal.SecurityTestResult
import de.deutschepost.sdm.cdlib.release.report.internal.Severity
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File

@RequiresTag("UnitTest")
@Tags("UnitTest")
internal class OdcReportTest : FunSpec({

    val pathPrefix = "src/test/resources/"
    val shortenedFilePath = pathPrefix + "odc/odc-carts-short.json"
    val shortFile = File(shortenedFilePath)
    val odcTestResult = parseJSON<OdcTestResult>(shortFile)

    test("OdcReport should contain scan info.") {
        odcTestResult.scanInfo.engineVersion shouldBe "6.1.6"
    }

    test("OdcReport should contain project info.") {
        odcTestResult.projectInfo.name shouldBe "carts"
    }

    test("Shortened OdcReport parse should contain 6 dependencies.") {
        odcTestResult.dependencies.size shouldBe 6
    }

    test("bcprov should have at least one vulnerability and no suppressed vulnerabilities.") {
        val bcprov = odcTestResult.dependencies.find {
            it.fileName == "bcprov-jdk15on-1.55.jar"
        }!!
        bcprov.vulnerabilities.shouldNotBeEmpty()
        bcprov.suppressedVulnerabilities.shouldBeEmpty()
    }

    test("Jackson Core should have at least one suppressed vulnerability and no vulnerabilities.") {
        val jackson = odcTestResult.dependencies.find {
            it.fileName == "jackson-core-2.8.6.jar"
        }!!
        jackson.suppressedVulnerabilities.shouldNotBeEmpty()
        jackson.vulnerabilities.shouldBeEmpty()
    }

    test("All vulnerabilities should have scores.") {
        odcTestResult.dependencies.shouldNotBeEmpty()
        odcTestResult.dependencies.forEach { dependency ->
            dependency.vulnerabilities.forAll { vulnerability ->
                vulnerability.internalSeverity shouldNotBe ""
            }
            dependency.suppressedVulnerabilities.forAll { vulnerability ->
                vulnerability.internalSeverity shouldNotBe ""
            }
        }
    }

    test("If there are vulnerabilities, they should have IDs.") {
        odcTestResult.dependencies.forAll { dependency ->
            if (dependency.vulnerabilities.isNotEmpty() || dependency.suppressedVulnerabilities.isNotEmpty()) {
                dependency.vulnerabilityIds shouldNotBe null
            }
        }
    }

    test("Test file should have 15 unique vulnerabilities") {
        val vulns = odcTestResult.dependencies.flatMap { it.vulnerabilities }
        vulns.size shouldBe 14
        val suppVulns = odcTestResult.dependencies.flatMap { it.suppressedVulnerabilities }
        suppVulns.size shouldBe 1
        val internal = SecurityTestResult.from(odcTestResult, "somefile.json")
        internal.vulnerabilities.openCount shouldBe vulns.size
        internal.vulnerabilities.suppressedCount shouldBe suppVulns.size
        internal.vulnerabilities.open.filter { it.severity == Severity.HIGH }.size shouldBe 7
    }

    test("Empty report should be parsed.") {
        val report = TestResultParser.parse(
            File("src/test/resources/passing/dependency-check-report.json")
        )
        report.shouldNotBeNull()
        report.shouldBeInstanceOf<SecurityTestResult>()
        report.vulnerabilities.open.shouldBeEmpty()
        report.vulnerabilities.suppressed.shouldBeEmpty()
    }
})
