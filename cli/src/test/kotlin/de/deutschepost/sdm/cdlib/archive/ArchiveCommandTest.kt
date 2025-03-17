package de.deutschepost.sdm.cdlib.archive

import de.deutschepost.sdm.cdlib.CdlibCommand
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.withEnvironment
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.string.shouldContain
import io.micronaut.configuration.picocli.PicocliRunner
import toArgsArray
import withErrorOutput
import withStandardOutput
import java.io.File

@RequiresTag("UnitTest")
@Tags("UnitTest")
class ArchiveCommandTest : BehaviorSpec({

    given("cdlib archive") {
        `when`("archive -h") {
            val args = "archive -h".toArgsArray()
            val (_, output) = withStandardOutput { PicocliRunner.run(CdlibCommand::class.java, *args) }

            then("should display help") {
                output shouldContain "Lets you interact with artifacts."
            }
        }

        `when`("archive upload -h") {
            val args = "archive upload -h".toArgsArray()
            val (_, output) = withStandardOutput { PicocliRunner.run(CdlibCommand::class.java, *args) }

            then("should display help") {
                output shouldContain "Uploads an artifact to Artifactory."
            }
        }

        `when`("archive upload") {
            val args = "".toArgsArray()
            val (_, output) = withErrorOutput {
                PicocliRunner.call(ArchiveCommand.UploadCommand::class.java, *args)
            }

            then("should complain about missing required parameters") {
                output shouldContain "Missing required options:"
            }
        }

        `when`("archive upload --repo-name=testRepo --artifactory-identity-token=xxxx --type build") {
            withEnvironment("CDLIB_RELEASE_NAME" to "", OverrideMode.SetOrOverride) {
                val args =
                    "--repo-name=testRepo --artifactory-identity-token=xxxx --type build --debug".toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(ArchiveCommand.UploadCommand::class.java, *args)
                }

                then("should complain about missing environment variable") {
                    ret shouldBeExactly -1
                    output shouldContain "Cannot infer folderName either specify --folder-name option or environment variable CDLIB_RELEASE_NAME."
                }
            }
        }

        `when`("archive upload --repo-name=testRepo --artifactory-identity-token=xxxx") {
            withEnvironment("CDLIB_RELEASE_NAME" to "irrelevant", OverrideMode.SetOrOverride) {
                val args =
                    "--repo-name=testRepo --artifactory-identity-token=xxxx --debug".toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(ArchiveCommand.UploadCommand::class.java, *args)
                }

                then("should complain about missing --type parameter") {
                    ret shouldBeExactly -1
                    output shouldContain "Parameter --type has not been specified!"
                    output shouldContain "Supported values are:"
                }
            }
        }

        `when`("archive upload --repo-name=testRepo --artifactory-identity-token=xxxx --type asdf") {
            withEnvironment("CDLIB_RELEASE_NAME" to "irrelevant", OverrideMode.SetOrOverride) {
                val args =
                    "--repo-name=testRepo --artifactory-identity-token=xxxx --type asdf --debug".toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(ArchiveCommand.UploadCommand::class.java, *args)
                }

                then("should complain about unsupported --type parameter") {
                    ret shouldBeExactly -1
                    output shouldContain "Unsupported parameter for --type: asdf"
                    output shouldContain "Supported values are:"
                }
            }
        }

        `when`("archive upload --repo-name=testRepo --artifactory-identity-token=xxxx --files fileNotExists") {
            val args =
                "--repo-name=testRepo --artifactory-identity-token=xxxx --files fileNotExists --debug".toArgsArray()
            val (ret, output) = withStandardOutput {
                PicocliRunner.call(ArchiveCommand.UploadCommand::class.java, *args)
            }

            then("should complain about missing files") {
                ret shouldBeExactly -1
                output shouldContain "fileNotExists does not exist."
            }
        }

        `when`("archive upload --repo-name=testRepo --artifactory-identity-token=xxxx --folder-name foo -f /**/*.pdf") {
            val args =
                "--repo-name=testRepo --artifactory-identity-token=xxxx --folder-name foo -f ${File.separatorChar}**${File.separatorChar}*.pdf --debug".toArgsArray()
            val (ret, output) = withStandardOutput {
                PicocliRunner.call(ArchiveCommand.UploadCommand::class.java, *args)
            }

            then("should complain about absolute path in Ant style pattern") {
                ret shouldBeExactly -1
                output shouldContain "Ant style pattern is only supported for relative paths."
            }
        }

        `when`("archive upload --repo-name=testRepo --artifactory-identity-token=xxxx --folder-name foo -f does/not/exist/*.pdf") {
            val args =
                "--repo-name=testRepo --artifactory-identity-token=xxxx --folder-name foo -f does${File.separatorChar}not${File.separatorChar}exist${File.separatorChar}*.pdf --debug".toArgsArray()
            val (ret, output) = withStandardOutput {
                PicocliRunner.call(ArchiveCommand.UploadCommand::class.java, *args)
            }

            then("should complain about missing files") {
                ret shouldBeExactly -1
                output shouldContain "Could not find any files matching pattern: "
            }
        }
    }
})
