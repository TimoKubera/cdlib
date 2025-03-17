package de.deutschepost.sdm.cdlib.change.metrics.client

import de.deutschepost.sdm.cdlib.change.changemanagement.ChangeTestHelper
import de.deutschepost.sdm.cdlib.change.changemanagement.model.ItSystem
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants
import de.deutschepost.sdm.cdlib.change.metrics.model.CdlibVersionConfig
import de.deutschepost.sdm.cdlib.change.metrics.model.CdlibVersionViewModel
import de.deutschepost.sdm.cdlib.change.metrics.model.Deployment
import de.deutschepost.sdm.cdlib.change.metrics.model.Release
import getSystemEnvironmentTestListenerWithOverrides
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.ZonedDateTime

@RequiresTag("IntegrationTest")
@Tags("IntegrationTest")
@MicronautTest
class CosmosDashboardRepositoryIntegrationTest(
    private val cosmosDashboardRepository: CosmosDashboardRepository,
    private val cosmosDashboardClient: CosmosDashboardClientTEST,
    private val cdlibVersionConfig: CdlibVersionConfig,
    private val changeTestHelper: ChangeTestHelper,
) : FunSpec() {
    override fun listeners() = listOf(getSystemEnvironmentTestListenerWithOverrides())


    init {
        test("Testing connection to CosmosDB is a success") {
            cosmosDashboardRepository.testConnection() shouldBe true
        }

        context("Adding deployment is a success") {
            val change = changeTestHelper.changeWithFullDefaults()
            val itSystem = ItSystem(
                "TEST",
                "TEST",
                "TEST",
                "TEST",
                JiraConstants.Criticality.NON_CRITICAL,
                ZonedDateTime.now().year
            )
            val deployment = Deployment(
                cdlibData = CdlibVersionViewModel(cdlibVersionConfig, true),
                deploymentType = "TEST",
                release = Release(cdlibVersionViewModel = CdlibVersionViewModel(cdlibVersionConfig, true)),
                status = Deployment.Status.FAILURE,
                itSystem = itSystem,
                change = change,
                gitops = false,
                deploymentLeadTimeInSeconds = null,
                isTest = true
            )

            test("Adding a deployment to CosmosDB NPI is a success") {
                shouldNotThrowAny {
                    cosmosDashboardRepository.addDeployment(deployment, true)
                }
            }

            test("Adding a deployment to CosmosDB PROD is a success") {
                shouldNotThrowAny {
                    cosmosDashboardRepository.addDeployment(deployment, false)
                }
            }
        }

        test("Receiving correct version information") {
            io.kotest.data.forAll(
                row("0.0.0-INTEGRATION-TEST", true, true),
                row("0.1.0", true, false),
                row("0.1", true, false),
                row("0.1.1", false, false),
                row("0.2.0", false, false)
            ) { version: String, isSupported: Boolean, isLatest: Boolean ->
                val versionInfo = cosmosDashboardClient.getVersionInfo(version)
                shouldNotThrowAny {
                    versionInfo.isSupported shouldBe isSupported
                    versionInfo.isLatest shouldBe isLatest
                }
            }
        }
    }
}
