package de.deutschepost.sdm.cdlib.release.report

import de.deutschepost.sdm.cdlib.release.report.external.fnci.FnciServiceRepository
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpStatus
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.zip.ZipInputStream

@RequiresTag("IntegrationTest")
@Tags("IntegrationTest")
@MicronautTest
class FnciClientIntegrationTest(
    @Value("\${fnci-token}") val token: String, private val fnciService: FnciServiceRepository
) : FunSpec() {
    private val projectId: Int = 274
    private val reportId = 1

    init {
        test("Get Project-ID from Project name") {
            val projectName = "19C_ICTO-3339_SDM_PhippyAndFriends"

            val projectID = fnciService.getId(projectName, token)

            projectID.shouldContain("Content: ", 274)

            println(projectID.getOrElse("Content: ") { print("getID failed"); -1 })

        }
        test("getNotApprovedInventory") {

            val inventory = fnciService.getProjectInventory(projectId, token)
            inventory.shouldNotBeEmpty()
            inventory.any { it.inventoryReviewStatus == "Approved" } shouldBe true
            println(inventory)

        }

        test("generateReport") {
            shouldNotThrowAny {
                val taskId = fnciService.generateReport(projectId, reportId, token)
                taskId shouldNotBe 0
            }
        }


        test("downloadReport") {
            val taskId = 29103
            val response = fnciService.downloadReport(projectId, reportId, taskId, token)
            //Check if request was successful
            response.status shouldBe HttpStatus.OK
            response.body.isPresent shouldBe true
            //Look for report file
            shouldNotThrowAny {
                val z = ZipInputStream(response.body.orElseThrow().inputStream())
                generateSequence {
                    z.nextEntry
                }.any { it.name.contains(".xlsx") } shouldBe true
            }
        }

        test("getProjectInformation") {
            val projectInformation = fnciService.getProjectInformation(projectId, token)
            projectInformation.id shouldBe projectId
        }

    }
}
