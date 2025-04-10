package de.deutschepost.sdm.cdlib.change

import de.deutschepost.sdm.cdlib.change.changemanagement.ChangeTestHelper
import de.deutschepost.sdm.cdlib.change.changemanagement.model.ItSystem
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants
import de.deutschepost.sdm.cdlib.change.metrics.model.*
import de.deutschepost.sdm.cdlib.utils.defaultObjectMapper
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.SystemEnvironmentTestListener
import io.kotest.matchers.equality.FieldsEqualityCheckConfig
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.ZonedDateTime
import java.util.*

@RequiresTag("UnitTest")
@Tags("UnitTest")
@MicronautTest
class DeploymentTest(
    private val changeTestHelper: ChangeTestHelper,
) : FunSpec() {
    override fun listeners() = listOf(
        SystemEnvironmentTestListener(
            mapOf(
                "CDLIB_RELEASE_NAME" to "releaseUnique_TEST",
                "CDLIB_RELEASE_NAME_UNIQUE" to "releaseUnique_TEST_123456",
                "CDLIB_APP_NAME" to "appName_TEST",
                "CDLIB_EFFECTIVE_BRANCH_NAME" to "branchName_TEST",
                "CDLIB_JOB_URL" to "https://test-url.jenkuns.example.com/jobUrl_TEST",
                "CDLIB_PM_GIT_MAIL" to "gitMail_TEST",
                "CDLIB_PM_GIT_NAME" to "gitName_TEST",
                "CDLIB_PM_GIT_ID" to "gitId_TEST",
                "CDLIB_PM_GIT_LINK" to "gitLink_TEST",
                "CDLIB_PM_GIT_MESSAGE" to "gitMessage_TEST",
                "CDLIB_PM_GIT_ORIGIN" to "gitOrigin_TEST",
                "CDLIB_CICD_PLATFORM" to "cicdPlatform_TEST",
                "CDLIB_PIPELINE_URL" to UUID.randomUUID().toString()
            ), OverrideMode.SetOrOverride
        )
    )


    init {
        val ctx = ApplicationContext.run(Environment.CLI, Environment.TEST, "test")
        val cdlibVersionConfig = ctx.getBean(CdlibVersionConfig::class.java)

        test("Last Commit should contain data from environment") {
            val l = Commit()
            l.id shouldBe System.getenv("CDLIB_PM_GIT_ID")
        }

        test("CdlibVersionData should contain cdlib versions data") {
            val c = cdlibVersionConfig
            c.cliReleaseVersion shouldBeEqualIgnoringCase "INTEGRATION-TEST-RV"
        }

        test("Deployment should be serializable and deserializable") {
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
                deploymentLeadTimeInSeconds = 0,
                isTest = true
            )
            val deploymentJson = deployment.toPrettyString()
            deploymentJson.shouldNotBeNull()
            println(deploymentJson)
            val parseDeployment =
                defaultObjectMapper.readValue(deploymentJson.byteInputStream(), Deployment::class.java)
            deployment.shouldBeEqualToComparingFields(
                parseDeployment,
                FieldsEqualityCheckConfig(propertiesToExclude = listOf(Deployment::date))
            )
            deployment.date.toInstant() shouldBe parseDeployment.date.toInstant()
        }

//        test("!Deployment should be deserializable") {
//            /**
//             * This test cannot work in the current implementation because of JsonUnwrapped.
//             * Make deployment parseable again
//             */
//            val deployment = Deployment(
//                cdlibVersionConfig = cdlibVersionConfig,
//                almId = "TEST",
//                itSystemName = "TEST",
//                deploymentType = "TEST",
//                status = Deployment.Status.FAILURE,
//            )
//            deployment shouldBeEqualToComparingFields objectMapper.readValue(
//                deployment.toPrettyString()?.byteInputStream(),
//                Deployment::class.java
//            )
//        }

        ctx.close()
    }
}
