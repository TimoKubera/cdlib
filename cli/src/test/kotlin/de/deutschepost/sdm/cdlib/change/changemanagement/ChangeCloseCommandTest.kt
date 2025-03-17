package de.deutschepost.sdm.cdlib.change.changemanagement

import de.deutschepost.sdm.cdlib.change.ChangeCommand
import de.deutschepost.sdm.cdlib.change.metrics.model.Deployment
import getSystemEnvironmentTestListenerWithOverrides
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import toArgsArray
import withErrorOutput

@RequiresTag("UnitTest")
@Tags("UnitTest")
@MicronautTest
class ChangeCloseCommandTest(
    @Value("\${change-management-token}") val token: String
) : StringSpec() {
    private val commercialReference = "5296"

    override fun listeners(): List<TestListener> {
        return listOf(
            getSystemEnvironmentTestListenerWithOverrides()
        )
    }

    init {
        "Publishing metrics without status fails" {

            val (_, output) = withErrorOutput {
                PicocliRunner.call(
                    ChangeCommand.CloseCommand::class.java,
                    *"--debug --test --jira-token $token --commercial-reference $commercialReference ".toArgsArray()
                )
            }

            output shouldContain "Missing required option: '--status=<statusStr>'"
        }

        "Status string from Azure Devops is correctly parsed to match the keywords used in metrics" {
            val statusSuccess = Deployment.Status.valueOf("SUCCEEDED").value
            statusSuccess shouldBe Deployment.Status.SUCCEEDED.value
            statusSuccess shouldBe Deployment.STATUS_SUCCESS

            val statusFailed = Deployment.Status.valueOf("FAILED").value
            statusFailed shouldBe Deployment.Status.FAILED.value
            statusFailed shouldBe Deployment.STATUS_FAILURE

            val statusCanceled = Deployment.Status.valueOf("CANCELED").value
            statusCanceled shouldBe Deployment.Status.CANCELED.value
            statusCanceled shouldBe Deployment.STATUS_ABORTED

            val statusSuccessWithIssues = Deployment.Status.valueOf("SUCCEEDEDWITHISSUES").value
            statusSuccessWithIssues shouldBe Deployment.Status.SUCCEEDEDWITHISSUES.value
            statusSuccessWithIssues shouldBe Deployment.STATUS_UNSTABLE
        }
    }
}
