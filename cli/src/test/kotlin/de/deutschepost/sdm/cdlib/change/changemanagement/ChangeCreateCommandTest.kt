package de.deutschepost.sdm.cdlib.change.changemanagement

import de.deutschepost.sdm.cdlib.change.ChangeCommand
import de.deutschepost.sdm.cdlib.change.metrics.model.Deployment
import getSystemEnvironmentTestListenerWithOverrides
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import toArgsArray
import withErrorOutput
import withStandardOutput
import java.lang.reflect.Method

@RequiresTag("UnitTest")
@Tags("UnitTest")
@MicronautTest
class ChangeCreateCommandTest(
    @Value("\${change-management-token}") val token: String
) : StringSpec() {
    override fun listeners(): List<TestListener> {
        return listOf(
            getSystemEnvironmentTestListenerWithOverrides()
        )
    }

    init {
        "Help Test" {
            val (_, output) = withStandardOutput {
                PicocliRunner.call(
                    ChangeCommand.CreateCommand::class.java,
                    *"--help".toArgsArray()
                )
            }
            println(output)
        }

        "Create change defaults are displayed correctly in cli error" {
            val (_, output) = withErrorOutput {
                PicocliRunner.call(
                    ChangeCommand.CreateCommand::class.java,
                    private companion object {
                        const val ILLEGAL_ARGUMENT_EXCEPTION = "java.lang.IllegalArgumentException"
                    }
                    
                    "Create change request fails due to missing commercial reference" {
                        val (_, output) = withErrorOutput {
                            PicocliRunner.call(
                                ChangeCommand.CreateCommand::class.java,
                                *"--test --jira-token $token".toArgsArray()
                            )
                        }
                    
                        output shouldContain "Missing required argument(s): --commercial-reference=<commercialReference>"
                    }
                    
                    "Command fails if webapproval is requested with missing parameters." {
                        val (exitCode, output) = withStandardOutput {
                            PicocliRunner.call(
                                ChangeCommand.CreateCommand::class.java,
                                *"--jira-token $token --debug --commercial-reference 5296 --test --webapproval --no-distribution".toArgsArray()
                            )
                        }
                        exitCode shouldBeExactly -1
                        output shouldContain ILLEGAL_ARGUMENT_EXCEPTION
                    }
                    
                    "Command fails if oslc but no distribution was passed" {
                        val (exitCode, output) = withStandardOutput {
                            PicocliRunner.call(
                                ChangeCommand.CreateCommand::class.java,
                                *"--jira-token $token --debug --commercial-reference 5296 --test".toArgsArray()
                            )
                        }
                        exitCode shouldBeExactly -1
                        output shouldContain ILLEGAL_ARGUMENT_EXCEPTION
                    }
                    
                    "Command doesn't fail if no-oslc and no distribution was passed" {
                        val (exitCode, output) = withStandardOutput {
                            PicocliRunner.call(
                                ChangeCommand.CreateCommand::class.java,
                                *"--jira-token $token --debug --commercial-reference 5296 --test --no-oslc".toArgsArray()
                            )
                        }
                        exitCode shouldBeExactly -1
                        output shouldContain ILLEGAL_ARGUMENT_EXCEPTION
                        output shouldContain "Verifying reports..."
                    }
                    
                    "Parsing deployment status string returns expected enum" {
                        listOf(
                            "SUCCESS", "FAILURE", "UNSTABLE", "ABORTED",
                            "Succeeded", "Failed", "Canceled", "SucceededWithIssues"
                        ).forAll {
                            val parseStatus: Method =
                                ChangeCommand.CloseCommand::class.java.getDeclaredMethod("parseStatus", String::class.java)
                            parseStatus.isAccessible = true
                            val parsedStatus = parseStatus.invoke(ChangeCommand.CloseCommand(), it) as Deployment.Status?
                            parsedStatus?.name shouldBe it.uppercase()
                        }
                    }
                    
    }
}
