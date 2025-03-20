package de.deutschepost.sdm.cdlib.change.changemanagement

import de.deutschepost.sdm.cdlib.change.ChangeCommand
import de.deutschepost.sdm.cdlib.change.changemanagement.api.ChangeHandler
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.ChangePhaseId.OPEN_TO_IMPLEMENTATION
import getSystemEnvironmentTestListenerWithOverrides
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.withEnvironment
import io.kotest.inspectors.forAll
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import toArgsArray
import withStandardOutput
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@RequiresTag("IntegrationTest")
@Tags("IntegrationTest")
@MicronautTest
class ChangeCloseIntegrationTest(
    @Value("\${change-management-token}") val token: String,
    private val changeTestHelper: ChangeTestHelper,
    private val changeHandler: ChangeHandler,
) : StringSpec() {

    private val status = "SUCCESS"
    private val commercialReference = "5296"
    private lateinit var changeDetails: ChangeCommand.CreateCommand.ChangeDetails
    override suspend fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        changeDetails = changeTestHelper.changeDetailsWithDefaults()
        changeHandler
            .initialise(
                "Bearer $token",
                isTestFlag = true,
                enforceFrozenZoneFlag = false,
                performWebapprovalFlag = false,
                performOslcFlag = false,
                gitopsFlag = false,
                skipApprovalWaitFlag = true
            )
            .findItSystem(commercialReference)
    }

    override fun listeners(): List<TestListener> = listOf(
        getSystemEnvironmentTestListenerWithOverrides(mapOf("CDLIB_PM_GIT_ORIGIN" to UUID.randomUUID().toString()))
    )

    init {
        "Closing change with commercial reference is a success" {
            changeHandler
                .post(changeDetails)
                .preauthorize()
                .transition(OPEN_TO_IMPLEMENTATION)

            val (exitCode, output) = withStandardOutput {
                PicocliRunner.call(
                    ChangeCommand.CloseCommand::class.java,
                    *"--debug --test --jira-token $token --commercial-reference $commercialReference --status $status".toArgsArray()
                )
            }

            output shouldContain "Retrieving IT system information (commercial reference, ALM-ID, name, key)."
            output shouldContain "Finding change to close for the current pipeline."
            output shouldNotContain "Could not find change to close for the current pipeline."
            output shouldContain "Found the following change to close: "
            output shouldContain "Closing change"
            output shouldNotContain "Could not transition to 'Under Review'."
            output shouldContain "Transitioning to next change request phase."
            output shouldContain "Change request phase transitioned successfully: 'Under Review'. Change was implemented successfully and is to be reviewed."
            output shouldContain "Pushing following metric object:"
            exitCode shouldBe 0
        }

        "Close change request fails when no change was created" {
            changeHandler
                .findExisting()
                .closeExisting()
            val (exitCode, closeOutput) = withStandardOutput {
                PicocliRunner.call(
                    ChangeCommand.CloseCommand::class.java,
                    *"--debug --test --jira-token $token --commercial-reference $commercialReference --status $status".toArgsArray()
                )
            }

            closeOutput shouldContain "Could not find change to close for the current pipeline."
            exitCode shouldBe -1
        }

        "Close change request fails and prints warnings when multiple changes were created" {
            changeHandler
                .post(changeDetails)
                .preauthorize()
                .transition(OPEN_TO_IMPLEMENTATION)
                .post(changeDetails)
                .preauthorize()
                .transition(OPEN_TO_IMPLEMENTATION)

            val (exitCode, closeOutput) = withStandardOutput {
                PicocliRunner.call(
                    ChangeCommand.CloseCommand::class.java,
                    *"--debug --test --jira-token $token --commercial-reference $commercialReference --status $status".toArgsArray()
                )
            }

            closeOutput shouldContain "Found multiple changes for this pipeline, please close them manually using the links below: "
            exitCode shouldBe -1
        }

        "Publishing metrics with invalid parameter status fails and prints warnings" {
            changeHandler
                .post(changeDetails)
                .preauthorize()
                .transition(OPEN_TO_IMPLEMENTATION)

            val (exitCode, output) = withStandardOutput {
                PicocliRunner.call(
                    ChangeCommand.CloseCommand::class.java,
                    *"--debug --test --jira-token $token --commercial-reference $commercialReference --status Invalid".toArgsArray()
                )
            }

            output shouldContain "Failed to parse Pipeline status"
            output shouldNotContain "Failed to create metric object."
            exitCode shouldBe -1
            changeTestHelper.closeChangeRequest(token, commercialReference)
        }

        "Publishing metrics with status: SUCCESS is a success" {
            changeHandler
                .post(changeDetails)
                .preauthorize()
                .transition(OPEN_TO_IMPLEMENTATION)
        
            val (exitCode, output) = withStandardOutput {
                PicocliRunner.call(
                    ChangeCommand.CloseCommand::class.java,
                    *"--debug --test --jira-token $token --commercial-reference $commercialReference --status SUCCESS".toArgsArray()
                )
            }
        
            output shouldNotContain "Failed to parse Pipeline status"
            output shouldNotContain "Failed to create metric object."
            output shouldContain Companion.PUSHING_METRIC_OBJECT
            exitCode shouldBe 0
        }
        
        "Change is not closed for failed status" {
            listOf("FAILED", "FAILURE").forAll {
                val (exitCode, output) = withStandardOutput {
                    changeHandler
                        .post(changeDetails)
                        .preauthorize()
                        .transition(OPEN_TO_IMPLEMENTATION)
                    PicocliRunner.call(
                        ChangeCommand.CloseCommand::class.java,
                        *"--test --jira-token $token --commercial-reference $commercialReference --status $it".toArgsArray()
                    )
                }
                output shouldNotContain "Closing change"
                output shouldContain "not closing change request."
                output shouldContain Companion.PUSHING_METRIC_OBJECT
                exitCode shouldBe 0
        
                changeHandler.findExisting().closeExisting()
                Thread.sleep(15.toDuration(DurationUnit.SECONDS).inWholeMilliseconds)
            }
        }
        
        companion object {
            const val PUSHING_METRIC_OBJECT = "Pushing following metric object:"
        }
                exitCode shouldBe 0


                changeHandler.findExisting().closeExisting()
                Thread.sleep(15.toDuration(DurationUnit.SECONDS).inWholeMilliseconds)
            }
        }

        "Closing change successfully publishes metrics via Jenkins" {
            withEnvironment(
                "CDLIB_JOB_URL" to "http://integration-test-url.jenkuns.example.com/foo/bar/job/1337",
                OverrideMode.SetOrOverride
            ) {
                changeHandler
                    .post(changeDetails)
                    .preauthorize()
                    .transition(OPEN_TO_IMPLEMENTATION)

                val (exitCode, output) = withStandardOutput {
                    PicocliRunner.call(
                        ChangeCommand.CloseCommand::class.java,
                        *"--test --jira-token $token --commercial-reference $commercialReference --status $status".toArgsArray()
                    )
                }

                output shouldContain "http://integration-test-url.jenkuns.example.com"
                output shouldContain "Dashboard status code: 201"
                exitCode shouldBeExactly 0
            }
        }

        "Closing change successfully publishes metrics via Jenkins as infrastructure deployment" {
            withEnvironment(
                "CDLIB_JOB_URL" to "http://integration-test-url.jenkuns.example.com/foo/bar/job/1337",
                OverrideMode.SetOrOverride
            ) {
                changeHandler
                    .post(changeDetails)
                    .preauthorize()
                    .transition(OPEN_TO_IMPLEMENTATION)

                val (exitCode, output) = withStandardOutput {
                    PicocliRunner.call(
                        ChangeCommand.CloseCommand::class.java,
                        *"--test --token $token --commercial-reference $commercialReference --status $status --deployment-type INFRA".toArgsArray()
                    )
                }

                output shouldContain "http://integration-test-url.jenkuns.example.com"
                output shouldContain "Dashboard status code: 201"
                output shouldContain "INFRA"
                exitCode shouldBeExactly 0
            }
        }

        "Closing change successfully publishes metrics via AzureDevOps" {
            val rnd = UUID.randomUUID().toString().substringBefore("-")

            withEnvironment(
                mapOf(
                    "CDLIB_JOB_URL" to "https://dev.azure.com/sw-zustellung-$rnd/ICTO-3339_SDM-phippyandfriends",
                    "CDLIB_PIPELINE_URL" to "https://dev.azure.com/sw-zustellung-$rnd/ICTO-3339_SDM-phippyandfriends/_build?definitionId=1337&branchFilter=superFeature",
                ),
                OverrideMode.SetOrOverride
            ) {
                changeHandler
                    .post(changeDetails)
                    .preauthorize()
                    .transition(OPEN_TO_IMPLEMENTATION)

                val (exitCode, output) = withStandardOutput {
                    PicocliRunner.call(
                        ChangeCommand.CloseCommand::class.java,
                        *"--test --jira-token $token --commercial-reference $commercialReference --status $status".toArgsArray()
                    )
                }

                output shouldContain "https://dev.azure.com/sw-zustellung-$rnd"
                output shouldContain "Dashboard status code: 201"
                exitCode shouldBeExactly 0
            }
        }

        "Closing change successfully when having a fuzzy matching job url" {
            val rnd = UUID.randomUUID().toString().substringBefore("-")

            withEnvironment(
                "CDLIB_PIPELINE_URL" to "https://test.dhl.com/job/$rnd/job/cli/job/i898-build-refactored-test-merged/display/redirect",
                OverrideMode.SetOrOverride
            ) {
                changeHandler
                    .post(changeDetails)
                    .preauthorize()
                    .transition(OPEN_TO_IMPLEMENTATION)
            }

            withEnvironment(
                mapOf(
                    "CDLIB_PIPELINE_URL" to "https://test.dhl.com/job/$rnd/job/cli/job/test/display/redirect",
                    "CDLIB_JOB_URL" to "https://dev\"CDLIB_JOB_URL\" to \"https://dev.azure.com/sw-zustellung-31b3183/ICTO-3339_SDM-phippyandfriends\",.azure.com/sw-zustellung-31b3183/ICTO-3339_SDM-phippyandfriends",
                ),
                OverrideMode.SetOrOverride
            ) {

                val (exitCode, output) = withStandardOutput {
                    changeHandler
                        .post(changeDetails)
                        .preauthorize()
                        .transition(OPEN_TO_IMPLEMENTATION)
                    PicocliRunner.call(
                        ChangeCommand.CloseCommand::class.java,
                        *"--test --jira-token $token --commercial-reference $commercialReference --status $status".toArgsArray()
                    )
                }

                output shouldContain "https://test.dhl.com/job/$rnd/job/cli/job/test/display/redirect"
                output shouldContain "https://dev.azure.com/sw-zustellung-31b3183"
                output shouldContain "Dashboard status code: 201"
                exitCode shouldBeExactly 0
            }
        }

        "Change close with custom comment is succesful and adds the comment." {
            val test = "test"
            val change = changeHandler
                .post(changeDetails)
                .preauthorize()
                .transition(OPEN_TO_IMPLEMENTATION)
                .getChange()

            val (_, output) = withStandardOutput {
                PicocliRunner.call(
                    ChangeCommand.CloseCommand::class.java,
                    *"--comment=$test --test --jira-token $token --commercial-reference $commercialReference --status $status".toArgsArray()
                )
            }

            val comments = changeHandler.getComments(change.id!!)
            comments.any { it.comment == test } shouldBe true
            output shouldContain "Adding custom comment to change."
        }

        "Change create and close with --gitops is a success" {
            val (ret0, _) = withStandardOutput {
                PicocliRunner.call(
                    ChangeCommand.CreateCommand::class.java,
                    *"--gitops --test --jira-token $token --commercial-reference $commercialReference --no-oslc --no-webapproval --no-tqs".toArgsArray()
                )
            }
            ret0 shouldBe 0

            val (ret1, out1) = withStandardOutput {
                changeHandler.findExisting()
            }
            // No change with pipelineUrl should exist
            out1 shouldContain "Could not find changes to close nor resume for the current pipeline."

            val (ret2, out2) = withStandardOutput {
                PicocliRunner.call(
                    ChangeCommand.CloseCommand::class.java,
                    *"--gitops --test --jira-token $token --commercial-reference $commercialReference --status $status".toArgsArray()
                )
            }
            // closing change with git repo url should be a success
            ret2 shouldBe 0
            out2 shouldContain """"gitops" : true"""
        }
    }
}
