package de.deutschepost.sdm.cdlib.release.report

import de.deutschepost.sdm.cdlib.release.report.external.FortifyTestResult
import de.deutschepost.sdm.cdlib.release.report.external.from
import de.deutschepost.sdm.cdlib.release.report.internal.SecurityTestResult
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

@RequiresTag("UnitTest")
@Tags("UnitTest")
class FortifyReportTest : FunSpec({

    val testFPRPath = "src/test/resources/fortify/fortify.fpr"

    test("FortifyReport companion should be able to extract Fortify XML.") {
        val fortifyXML = withContext(Dispatchers.IO) {
            FortifyTestResult.parseFortifyArchive(FileInputStream(testFPRPath)).first
        }
        fortifyXML.shouldNotBeNull()
        fortifyXML.engineData.ruleInfo.shouldNotBeNull()
        fortifyXML.engineData.ruleInfo.shouldNotBeEmpty()
    }

    test("FortifyReport companion should be able to extract Fortify XML from parrot.") {
        val fortifyXML = withContext(Dispatchers.IO) {
            FortifyTestResult.parseFortifyArchive(FileInputStream("src/test/resources/fortify/fortify_parrot.fpr")).first
        }
        fortifyXML.shouldNotBeNull()
        fortifyXML.engineData.ruleInfo.shouldNotBeNull()
        fortifyXML.engineData.ruleInfo.shouldNotBeEmpty()
        shouldNotThrowAny {
            SecurityTestResult.from(fortifyXML to emptyList(), "src/test/resources/fortify/fortify_parrot.fpr")
        }
    }

    test("FortifyReport companion should throw an exception and log output.") {
        shouldThrow<NoSuchElementException> {
            FortifyTestResult.parseFortifyArchive(FileInputStream("src/test/resources/fortify/broken.fpr")).first
        }
    }

    test("ExternalFortifyReport should parse fvdl and suppressions.") {
        val pair = withContext(Dispatchers.IO) {
            FortifyTestResult.parseFortifyArchive(FileInputStream("src/test/resources/passing/fortify.fpr"))
        }
        pair.second.shouldNotBeEmpty()
        pair.first.shouldNotBeNull()
    }

    test("ExternalFortifyReport parsing should not fail when there are no suppressions.") {
        val pair = withContext(Dispatchers.IO) {
            FortifyTestResult.parseFortifyArchive(FileInputStream("src/test/resources/fortify/fortify.fpr"))
        }
        pair.second.shouldBeEmpty()
        pair.first.shouldNotBeNull()
    }

    test("ExternalFortifyReport parsing should fail when there is no fvdl file.") {
        shouldThrow<NoSuchElementException> {
            withContext(Dispatchers.IO) {
                FortifyTestResult.parseFortifyArchive(FileInputStream("src/test/resources/fortify/broken.fpr"))
            }
        }
    }

    test("Check able to parse fvdl.") {
        val audit = File("src/test/resources/fortify/audit.fvdl").readText()
        shouldNotThrowAny {
            FortifyTestResult.FPRHandler.parseFortifyXML(audit)
        }
    }

    test("Check missing snippet and path") {
        val audit = File("src/test/resources/fortify/audit-small.fvdl").readText()
        shouldNotThrowAny {
            FortifyTestResult.FPRHandler.parseFortifyXML(audit)
        }
    }
})
