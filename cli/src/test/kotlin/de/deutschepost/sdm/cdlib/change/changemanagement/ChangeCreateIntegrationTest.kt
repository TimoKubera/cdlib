package de.deutschepost.sdm.cdlib.change.changemanagement

import de.deutschepost.sdm.cdlib.CdlibCommand
import de.deutschepost.sdm.cdlib.change.ChangeCommand
import de.deutschepost.sdm.cdlib.change.changemanagement.api.ChangeHandler
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.ApprovalStatus
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.Category.HOUSEKEEPING
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.ChangePhaseId.OPEN_TO_IMPLEMENTATION
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.ChangeStatus.AWAITING_IMPLEMENTATION
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.ChangeStatus.WAITING_FOR_APPROVAL
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.ChangeType.MAJOR
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.Criticality.OPERATIONAL
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldDefaults.APPROVAL_CHECK_TIMEOUT_IN_MINUTES
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.ImpactClass.NONE
import getSystemEnvironmentTestListenerWithOverrides
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCase
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.withEnvironment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Value
import io.micronaut.context.env.Environment
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.unmockkAll
import toArgsArray
import withErrorOutput
import withMockedVersionInfo
import withStandardOutput
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@RequiresTag("IntegrationTest")
@Tags("IntegrationTest")
@MicronautTest
class ChangeCreateIntegrationTest(
    @Value("\${change-management-token}") val token: String,
    private val changeTestHelper: ChangeTestHelper,
    private val changeHandler: ChangeHandler
) : FunSpec() {

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

    override fun listeners(): List<TestListener> {
        return listOf(
            getSystemEnvironmentTestListenerWithOverrides()
        )
    }

    init {
        test("testing unsupported CDLib version preventing preauthorization") {
            ApplicationContext.run(
                Environment.CLI, Environment.TEST, "ver02"
            ).use { ctx ->
                val (_, output) = withStandardOutput {
                    PicocliRunner.run(
                        CdlibCommand::class.java,
                        ctx,
                        *"change create --test --skip-approval-wait --jira-token $token --no-oslc --no-webapproval --no-tqs --commercial-reference $commercialReference".toArgsArray()
                    )
                }
                output shouldContain "CDLib version 0.2.0-INTEGRATION-TEST is not supported anymore. Please update to a newer version. Pre-authorization is not possible."
                output shouldContain "A new version of CDLib is available"

                output shouldContain "Retrieving IT system information"
                output shouldContain "Searching existing changes for the current pipeline"
                output shouldContain "Could not find changes to close nor resume for the current pipeline"
                output shouldContain "Posting change request"
                output shouldContain "Determining whether change can be preauthorized."
                output shouldContain "  CDLib version is supported: false\n" +
                    "  Impact Class: ${NONE.name}\n" +
                    "  Business Criticality: ${OPERATIONAL.name}\n" +
                    "  Determined change type --> MINOR"
                output shouldContain "Updating change request type: MINOR"
                output shouldContain "Transitioning change request phase: ${OPEN_TO_IMPLEMENTATION.name}"
                output shouldContain "Checking change request status for approval every "
                output shouldContain "Checked current change request status: ${WAITING_FOR_APPROVAL.name}"

            }
            changeTestHelper.closeChangeRequest(
                token,
                commercialReference
            )
        }

        withMockedVersionInfo(changeHandler) {
            test("Creating change via full change command fails given an invalid commercial reference") {
                val (_, output) = withStandardOutput {
                    PicocliRunner.run(
                        CdlibCommand::class.java,
                        *"change create --test --skip-approval-wait --no-oslc --no-webapproval --no-tqs --jira-token $token --commercial-reference DI-123456".toArgsArray()
                    )
                }
                output shouldContain "Failed to get commercial reference: DI-123456"
            }

            test("Change created with wrong parameters and --trace option extends logging") {
                val (_, output) = withStandardOutput {
                    val args: Array<String> =
                        "change create --skip-approval-wait --jira-token wrong --no-oslc --no-webapproval --no-tqs --debug --trace --commercial-reference $commercialReference --test".toArgsArray()
                    PicocliRunner.run(CdlibCommand::class.java, *args)
                }

                output shouldContain "TRACE"
            }

            test("Change create command with no open changes for the current pipeline continues successfully with appropriate log") {
                val (_, output) = withStandardOutput {
                    PicocliRunner.run(
                        CdlibCommand::class.java,
                        *"change create --test --skip-approval-wait --no-oslc --no-webapproval --no-tqs --jira-token $token --commercial-reference $commercialReference".toArgsArray()
                    )
                }

                output shouldContain "Retrieving IT system information"
                output shouldContain "Searching existing changes for the current pipeline"
                output shouldContain "Could not find changes to close nor resume for the current pipeline"
                output shouldContain "Posting change request"
                output shouldContain "Determining whether change can be preauthorized."
                output shouldContain "  Impact Class: ${NONE.name}\n" +
                    "  Business Criticality: ${OPERATIONAL.name}\n" +
                    "  Determined change type --> ${ApprovalStatus.PREAUTHORIZED.name}"
                output shouldContain "Updating change request type: ${ApprovalStatus.PREAUTHORIZED.name}"
                output shouldContain "Transitioning change request phase: ${OPEN_TO_IMPLEMENTATION.name}"
                output shouldContain "Checking change request status for approval every "
                output shouldContain "Checked current change request status: ${AWAITING_IMPLEMENTATION.name}"



                changeTestHelper.closeChangeRequest(token, commercialReference)
            }

            context("Change create command with custom details...") {
                test("...and wrong category fails.") {
                    val (_, output) = withErrorOutput {
                        val toArgsArray = """change create
                        | --category=wrongCategory
                        | --test --skip-approval-wait --no-oslc --no-webapproval --no-tqs --jira-token $token
                        | --commercial-reference $commercialReference""".trimMargin().replace("\n", "")
                            .toArgsArray()
                        PicocliRunner.run(
                            CdlibCommand::class.java,
                            *toArgsArray
                        )
                    }
                    output shouldContain "Invalid value for option '--category': expected one of [ROLLOUT, NO_ROLLOUT, AUTHORIZATIONS, DATA_MAINTENANCE, TECHNICAL_REQUIREMENTS, LEGAL_OR_CONTRACTUAL_REQUIREMENTS, HOUSEKEEPING, CAPACITY_ADJUSTMENTS, SECURITY, TROUBLESHOOTING, OTHER] (case-sensitive) but was 'wrongCategory'"
                }

                test("...and wrong impactClass fails.") {
                    val (_, output) = withErrorOutput {
                        val toArgsArray = """change create
                        | --impact-class=wrongImpactClass
                        | --test --skip-approval-wait --no-oslc --no-webapproval --no-tqs --jira-token $token
                        | --commercial-reference $commercialReference""".trimMargin().replace("\n", "")
                            .toArgsArray()
                        PicocliRunner.run(
                            CdlibCommand::class.java,
                            *toArgsArray
                        )
                    }

                    output shouldContain "Invalid value for option '--impact-class': expected one of [CRITICAL, HIGH, LOW, MEDIUM, NONE] (case-sensitive) but was 'wrongImpactClass'"
                }

                test("...is successful with appropriate log and custom comment.") {
                    val test = "test"
                    val toArgsArray = """change create
                        | --category $HOUSEKEEPING
                        | --summary $test
                        | --description $test
                        | --impact-class $NONE
                        | --impact $test
                        | --target $test
                        | --fallback $test
                        | --implementation-risk $test
                        | --omission-risk $test
                        | --test --skip-approval-wait --no-oslc --no-webapproval --no-tqs --jira-token $token
                        | --commercial-reference $commercialReference""".trimMargin().replace("\n", "")
                        .toArgsArray()
                    PicocliRunner.run(
                        CdlibCommand::class.java,
                        *toArgsArray
                    )

                    val change = changeHandler
                        .findExisting()
                        .findResumable()
                        .getChange()

                    change.category shouldBe HOUSEKEEPING
                    change.summary shouldBe test
                    change.description shouldBe test
                    change.impactClass shouldBe NONE
                    change.impact shouldBe test
                    change.target shouldBe test
                    change.fallback shouldBe test
                    change.implementationRisk shouldBe test
                    change.omissionRisk shouldBe test

                    changeHandler.closeExisting()
                }
            }

            test("Change create command with resume flag and changes open for the current pipeline closes all but the latest one and resumes that one successfully") {
                withEnvironment(
                    "CDLIB_JOB_URL" to "https://integration-test-url.jenkuns.example.com/foo/bar/job/1336",
                    OverrideMode.SetOrOverride
                ) {
                    val changeDetails = changeTestHelper.changeDetailsWithDefaults()
                    changeHandler
                        .post(changeDetails)
                        .preauthorize()
                        .transition(OPEN_TO_IMPLEMENTATION)
                        .post(changeDetails)
                        .preauthorize()
                        .transition(OPEN_TO_IMPLEMENTATION)
                }

                val (_, output) = withStandardOutput {
                    withEnvironment(
                        "CDLIB_JOB_URL" to "https://integration-test-url.jenkuns.example.com/foo/bar/job/1337",
                        OverrideMode.SetOrOverride
                    ) {
                        PicocliRunner.run(
                            CdlibCommand::class.java,
                            *"change create --resume true --test --skip-approval-wait --no-oslc --no-webapproval --no-tqs --jira-token $token --commercial-reference $commercialReference".toArgsArray()
                        )
                    }
                }

                output shouldContain "Starting Change Management process."
                output shouldContain "Retrieving IT system information"
                output shouldContain "Searching existing changes for the current pipeline"
                output shouldContain "Closing change:"
                output shouldContain "Adding abort comment to change..."
                output shouldContain "Resuming change: "
                output shouldContain "Adding resume comment to change..."
                output shouldContain "Adding new job url to change description..."
                output shouldContain "Checking change request status for approval every "
                output shouldContain "Checked current change request status: ${AWAITING_IMPLEMENTATION.name}"
                output shouldNotContain "Posting change request: "
                output shouldNotContain "Change request was not approved after $APPROVAL_CHECK_TIMEOUT_IN_MINUTES minutes."


                changeTestHelper.closeChangeRequest(token, commercialReference)
            }

            test("Change create command with open changes that are awaiting implementation transitions them to 're-plan', cancels and then closes them") {

                withEnvironment(
                    "CDLIB_JOB_URL" to "https://integration-test-url.jenkuns.example.com/foo/bar/job/1336",
                    OverrideMode.SetOrOverride
                ) {
                    changeHandler
                        .post(changeDetails)
                        .preauthorize()
                        .transition(OPEN_TO_IMPLEMENTATION)
                        .post(changeDetails)
                        .preauthorize()
                        .transition(OPEN_TO_IMPLEMENTATION)
                }

                val (_, output) = withStandardOutput {
                    withEnvironment(
                        "CDLIB_JOB_URL" to "https://integration-test-url.jenkuns.example.com/foo/bar/job/1337",
                        OverrideMode.SetOrOverride
                    ) {
                        PicocliRunner.run(
                            CdlibCommand::class.java,
                            *"change create --test --skip-approval-wait --debug --no-oslc --no-webapproval --no-tqs --jira-token $token --commercial-reference $commercialReference".toArgsArray()
                        )
                    }
                }

                output shouldContain "Retrieving IT system information"
                output shouldContain "Searching existing changes for the current pipeline"
                output shouldContain "Closing change:"
                output shouldContain "Rescheduling..."
                output shouldContain "Cancelling..."
                output shouldContain "Closing..."
                output shouldContain "Adding abort comment to change..."
                output shouldContain "Posting change request"
                output shouldContain "Determining whether change can be preauthorized."
                output shouldContain "  Impact Class: ${NONE.name}\n" +
                    "  Business Criticality: ${OPERATIONAL.name}\n" +
                    "  Determined change type --> ${ApprovalStatus.PREAUTHORIZED.name}"
                output shouldContain "Updating change request type: ${ApprovalStatus.PREAUTHORIZED.name}"
                output shouldContain "Transitioning change request phase: ${OPEN_TO_IMPLEMENTATION.name}"
                output shouldContain "Checking change request status for approval every "
                output shouldContain "Checked current change request status: ${AWAITING_IMPLEMENTATION.name}"
                output shouldNotContain "Could not find changes to close nor resume for the current pipeline"

                changeTestHelper.closeChangeRequest(token, commercialReference)
            }

            context("Change create command during frozen zone") {
                val (_, output) = withStandardOutput {
                    PicocliRunner.run(
                        CdlibCommand::class.java,
                        *"change create --test --skip-approval-wait --start=2023-01-01T00:00:00+01:00 --no-oslc --no-webapproval --no-tqs --enforce-frozen-zone --jira-token $token --commercial-reference $commercialReference".toArgsArray()
                    )
                }

                test("...creates a change and progresses it regularly") {
                    output shouldContain "Retrieving IT system information"
                    output shouldContain "Searching existing changes for the current pipeline"
                    output shouldContain "Posting change request"
                }

                test("...ignores custom change window.") {
                    output shouldContain "Frozen zone active: Ignoring set custom change window."
                }

                test("...recognises frozen zone") {
                    output shouldContain "Determining whether change can be preauthorized."
                    output shouldContain "  Impact Class: ${NONE.name}\n" +
                        "  Business Criticality: ${OPERATIONAL.name}\n" +
                        "  Special conditions:\n" +
                        "    Frozen Zone (i.e. Starkverkehr) is active from "
                }

                test("...updates change type to major") {
                    output shouldContain "  Determined change type --> ${MAJOR.name}"
                    output shouldContain "Updating change request type: ${MAJOR.name}"
                }

                test("...and does not preauthorize nor approve it") {
                    output shouldContain "Transitioning change request phase: ${OPEN_TO_IMPLEMENTATION.name}"
                    output shouldContain "Checked current change request status: ${WAITING_FOR_APPROVAL.name}"
                    output shouldNotContain "Checked current change request status: ${AWAITING_IMPLEMENTATION.name}"
                    output shouldNotContain "Updating change request type: ${ApprovalStatus.PREAUTHORIZED.name}"
                }

                changeTestHelper.closeChangeRequest(token, commercialReference)
            }

            context("Change create with custom change window...") {
                val now = ZonedDateTime.now()
                val (_, output) = withStandardOutput {
                    PicocliRunner.run(
                        CdlibCommand::class.java,
                        *("change create --test --skip-approval-wait --no-oslc --no-webapproval --no-tqs --jira-token $token " +
                            "--commercial-reference $commercialReference " +
                            "--start ${now.toIso()} " +
                            "--end ${now.plusDays(2).toIso()}").toArgsArray(),
                    )
                }

                test("...creates a change and progresses it regularly") {

                    output shouldContain "Retrieving IT system information"
                    output shouldContain "Searching existing changes for the current pipeline"
                    output shouldContain "Could not find changes to close nor resume for the current pipeline"
                    output shouldContain "Posting change request"
                    output shouldContain "Determining whether change can be preauthorized."
                    output shouldContain "  Impact Class: ${NONE.name}\n" +
                        "  Business Criticality: ${OPERATIONAL.name}\n" +
                        "  Determined change type --> ${ApprovalStatus.PREAUTHORIZED.name}"
                    output shouldContain "Updating change request type: ${ApprovalStatus.PREAUTHORIZED.name}"
                    output shouldContain "Transitioning change request phase: ${OPEN_TO_IMPLEMENTATION.name}"
                    output shouldContain "Checking change request status for approval every "
                    output shouldContain "Checked current change request status: ${AWAITING_IMPLEMENTATION.name}"
                }

                test("...exits when change window is over on a resume run") {
                    val expiredChangeDetails = changeTestHelper.changeDetailsWithDefaults()
                    expiredChangeDetails.startOpt = now.minusHours(5)
                    expiredChangeDetails.endOpt = now.minusHours(4)

                    changeHandler
                        .post(expiredChangeDetails)
                        .preauthorize()
                        .transition(OPEN_TO_IMPLEMENTATION)

                    val (_, out) = withStandardOutput {
                        withMockedVersionInfo(changeHandler) {
                            PicocliRunner.run(
                                CdlibCommand::class.java,
                                *"change create --resume true --no-oslc --no-webapproval --no-tqs --commercial-reference $commercialReference --test --skip-approval-wait --jira-token $token".toArgsArray()
                            )
                        }
                    }

                    out shouldNotContain "Frozen zone (i.e. Starkverkehr) in effect from" // cut off due to dynamic date
                    out shouldContain "Change window has expired ("
                    out shouldContain "therefore cannot\nresume and closing this change, please create a new change by re-triggering this run.\nAborting pipeline..."

                }
            }

            context("Create change with wrong custom change window...") {
                val currentTime = ZonedDateTime.now()

                test("...exits when start time is before the current time") {
                    val (_, out) = withStandardOutput {
                        PicocliRunner.run(
                            CdlibCommand::class.java,
                            *("change create --test --no-oslc --no-webapproval --no-tqs --jira-token $token " +
                                "--commercial-reference $commercialReference " +
                                "--start ${currentTime.minusDays(2).toIso()} " +
                                "--end ${currentTime.plusHours(2).toIso()}").toArgsArray(),
                        )
                    }

                    out shouldContain "Start date cannot be more than 24 hours in the past."
                }

                test("...exits when end time is before start time") {
                    val (_, out) = withStandardOutput {
                        PicocliRunner.run(
                            CdlibCommand::class.java,
                            *("change create --test --skip-approval-wait --no-oslc --no-webapproval --no-tqs --jira-token $token " +
                                "--commercial-reference $commercialReference " +
                                "--start ${currentTime.toIso()} " +
                                "--end ${currentTime.minusHours(2).toIso()}").toArgsArray()
                        )
                    }

                    out shouldContain "End date needs to be later than the start date."
                }
            }
        }


        test("Change create command with custom approval interval uses the interval successfully.") {
            val approvalCheckInterval = 1
            val (_, output) = withStandardOutput {
                PicocliRunner.run(
                    CdlibCommand::class.java,
                    *"change create --test --skip-approval-wait --approval-interval-in-minutes=$approvalCheckInterval --no-oslc --no-webapproval --no-tqs --jira-token $token --commercial-reference $commercialReference".toArgsArray()
                )
            }

            output shouldContain "Checking change request status for approval every ${approvalCheckInterval}m."
        }

        test("Change create with custom comment is successful and adds the comment.") {
            val test = "test"
            val (_, output) = withStandardOutput {
                val toArgsArray = """change create
                            | --comment=$test
                            | --test --skip-approval-wait --no-oslc --no-webapproval --no-tqs --jira-token $token
                            | --commercial-reference $commercialReference""".trimMargin().replace("\n", "")
                    .toArgsArray()
                PicocliRunner.run(
                    CdlibCommand::class.java,
                    *toArgsArray
                )
            }

            val change = changeHandler
                .findExisting()
                .findResumable()
                .getChange()
            val comments = changeHandler.getComments(change.id!!)
            comments.any { it.comment == test } shouldBe true
            output shouldContain "Adding custom comment to change."
        }

        test("Change create with custom change approval window is successful.") {
            val interval = 1
            val (_, output) = withStandardOutput {
                val toArgsArray = """change create
                            | --approval-interval-in-minutes=$interval
                            | --test --no-oslc --no-webapproval --no-tqs --jira-token $token
                            | --commercial-reference $commercialReference""".trimMargin().replace("\n", "")
                    .toArgsArray()
                PicocliRunner.run(
                    CdlibCommand::class.java,
                    *toArgsArray
                )
            }

            output shouldContain "Checking change request status for approval every ${interval}m."
        }
    }

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        unmockkAll()
    }

    private fun ZonedDateTime.toIso(): String? =
        this.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
}
