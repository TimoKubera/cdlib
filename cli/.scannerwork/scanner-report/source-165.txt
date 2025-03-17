package de.deutschepost.sdm.cdlib.change.sharepoint

import com.fasterxml.jackson.module.kotlin.readValue
import de.deutschepost.sdm.cdlib.change.changemanagement.model.ItSystem
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants
import de.deutschepost.sdm.cdlib.change.sharepoint.model.SharepointOslcListItem
import de.deutschepost.sdm.cdlib.release.report.external.FnciTestResult
import de.deutschepost.sdm.cdlib.release.report.external.fnci.FnciInventoryItem
import de.deutschepost.sdm.cdlib.release.report.external.fnci.FnciProjectInfo
import de.deutschepost.sdm.cdlib.release.report.external.fnci.FnciProjectInfoWrapper
import de.deutschepost.sdm.cdlib.release.report.external.from
import de.deutschepost.sdm.cdlib.release.report.internal.OslcTestResult
import de.deutschepost.sdm.cdlib.utils.permissiveObjectMapper
import getSystemEnvironmentTestListenerWithOverrides
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.io.File
import java.time.ZonedDateTime

@RequiresTag("IntegrationTest")
@Tags("IntegrationTest")
@MicronautTest
class SharepointOnlineOslcRepositoryIntegrationTest(
    private val sharepointOnlineOslcRepository: SharepointOnlineOslcRepository,
    graphTokenProvider: GraphTokenProvider
) : FunSpec() {
    private val projectPath = "src/test/resources/fnci/fnci-project.json"
    private val inventoryPath = "src/test/resources/fnci/fnci-inventory.json"
    private val itSystem = ItSystem(
        commercialReferenceNumber = "5296",
        name = "SDM",
        key = "",
        almId = "ICTO-3339",
        criticality = JiraConstants.Criticality.NON_CRITICAL,
        businessYear = ZonedDateTime.now().year
    )

    override fun listeners(): List<TestListener> = listOf(
        getSystemEnvironmentTestListenerWithOverrides()
    )

    init {
        val accessToken = graphTokenProvider.token
        test("authenticate") {

            accessToken shouldContain "ey"
        }

        test("addEntryTest") {
            val projectInfo: FnciProjectInfo =
                permissiveObjectMapper.readValue(File(projectPath), FnciProjectInfoWrapper::class.java).data
            val inventory: List<FnciInventoryItem> = permissiveObjectMapper.readValue(File(inventoryPath))
            val report =
                OslcTestResult.from(FnciTestResult(projectInfo, inventory), "src/test/resources/fnci/fnci-project.json")
            val listItem = SharepointOslcListItem(
                reportUrl = "https://test-url.jenkuns.example.com/jobURL_TEST",
                oslcTestResult = report,
                itSystem = itSystem,
                appName = "cli",
                isDistribution = false
            )
            shouldNotThrowAny {
                sharepointOnlineOslcRepository.addEntryTest(listItem)
            }
        }
    }
}
