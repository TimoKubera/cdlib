package de.deutschepost.sdm.cdlib.release.report

import de.deutschepost.sdm.cdlib.release.report.external.ZapTestResult
import de.deutschepost.sdm.cdlib.release.report.internal.SecurityTestResult
import de.deutschepost.sdm.cdlib.release.report.internal.Vulnerability
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File

@RequiresTag("UnitTest")
@Tags("UnitTest")
internal class ZapReportTest : FunSpec({

    val pathPrefix = "src/test/resources/"
    val zapSuppressedFilePath = pathPrefix + "zap/zap-report_all_suppressed.json"
    val zapOpenFilePath = pathPrefix + "zap/zap-report_with_open.json"
    val zapSuppressedReport = parseJSON<ZapTestResult>(File(zapSuppressedFilePath))
    val zapOpenReport = parseJSON<ZapTestResult>(File(zapOpenFilePath))

    test("SuppressedFile should have no open issues.") {
        zapSuppressedReport.sites.forEach {
            it.getOpenVulnerabilityList().count() shouldBe 0
            it.getSuppressedVulnerabilityList().count() shouldBe 14
        }
    }

    test("OpenFile should have exactly two open issues.") {
        zapOpenReport.sites.forEach {
            print(it.getOpenVulnerabilityList())
            it.getOpenVulnerabilityList().count() shouldBe 4
        }
    }

    context("Local suppressions file should") {
        val report = TestResultParser.parse(
            File("src/test/resources/zap/zap-dast-front-end-local-supps.json"),
        )
        report.shouldNotBeNull()
        report.shouldBeInstanceOf<SecurityTestResult>()
        test("have 2 open and 2 suppressed vulnerabilities") {
            report.vulnerabilities.open.count() shouldBe 2
            report.vulnerabilities.suppressed.count() shouldBe 10
        }
        test("have 1 vulnerability twice") {
            report.vulnerabilities.suppressed.count { (it as Vulnerability).id == "10202" } shouldBe 1
            report.vulnerabilities.open.count { it.id == "10202" } shouldBe 1
        }
    }

    test("Empty file should have exactly 0 issues.") {
        val report = TestResultParser.parse(File(pathPrefix + "passing/zap.json"))
        report.shouldNotBeNull()
        report.shouldBeInstanceOf<SecurityTestResult>()
        report.vulnerabilities.open.shouldBeEmpty()
        report.vulnerabilities.suppressed.shouldBeEmpty()
    }
})
