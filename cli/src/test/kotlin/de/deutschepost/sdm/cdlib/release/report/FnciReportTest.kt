package de.deutschepost.sdm.cdlib.release.report

import com.fasterxml.jackson.module.kotlin.readValue
import de.deutschepost.sdm.cdlib.release.report.external.FnciTestResult
import de.deutschepost.sdm.cdlib.release.report.external.fnci.FnciInventoryItem
import de.deutschepost.sdm.cdlib.release.report.external.fnci.FnciProjectInfo
import de.deutschepost.sdm.cdlib.release.report.external.fnci.FnciProjectInfoWrapper
import de.deutschepost.sdm.cdlib.release.report.external.from
import de.deutschepost.sdm.cdlib.release.report.internal.OslcComplianceStatus
import de.deutschepost.sdm.cdlib.release.report.internal.OslcTestResult
import de.deutschepost.sdm.cdlib.utils.defaultObjectMapper
import de.deutschepost.sdm.cdlib.utils.permissiveObjectMapper
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import java.io.File

@RequiresTag("UnitTest")
@Tags("UnitTest")
class FnciReportTest : FunSpec({
    class FnciReportTest : FunSpec({
        companion object {
            private const val PROJECT_PATH = "src/test/resources/fnci/fnci-project.json"
        }
    
        val projectPath = PROJECT_PATH
        val inventoryPath = "src/test/resources/fnci/fnci-inventory.json"
        context("Project Info") {
            test("Parses projectInfo correctly") {
                val projectInfo = permissiveObjectMapper.readValue(
                    File(projectPath), FnciProjectInfoWrapper::class.java
                ).data
                projectInfo.policyProfileName shouldBe "Non-Distribution insecure"
                defaultObjectMapper.writeValue(System.out, projectInfo)
            }
        }
    
        context("OslcTestResult") {
            val projectInfo: FnciProjectInfo =
                permissiveObjectMapper.readValue(File(PROJECT_PATH), FnciProjectInfoWrapper::class.java).data
            val inventory: List<FnciInventoryItem> = permissiveObjectMapper.readValue(File(inventoryPath))
            )
            report.complianceStatus shouldBe OslcComplianceStatus.GREEN
            report.unapprovedItems.shouldBeEmpty()
            defaultObjectMapper.writerWithDefaultPrettyPrinter().writeValue(System.out, report)
        }

        test("Canonical file name should depend on project, not files") {
            val report = OslcTestResult.from(
                FnciTestResult(projectInfo, inventory.filter { it.inventoryReviewStatus == "Approved" }),
                "src/test/resources/fnci/fnci-project.json"
            )
            report.canonicalFilename shouldEndWith "${report.projectName}.json"
        }
    }
})
